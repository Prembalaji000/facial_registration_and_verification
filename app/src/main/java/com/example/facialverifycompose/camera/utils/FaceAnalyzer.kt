package com.example.facialverifycompose.camera.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import com.example.facialverifycompose.FacePosition
import kotlin.math.hypot

class FaceAnalyzer(
    private val onFaceDetected: (faces: List<Face>, width: Int, height: Int) -> Unit,
    private val faceNetProcessor: FaceNetProcessor,
    private val livenessChecker: LivenessChecker,
    private val onStatusChanged: (FaceMonitorStatus) -> Unit,
    private val onDescriptorGenerated: (FloatArray) -> Unit,
    private val addCapturedFace: (Bitmap?) -> Unit,
    private val getTargetPosition: () -> FacePosition? = { null },
    private val isRegisterMode: () -> Boolean = { true }
) : ImageAnalysis.Analyzer {

    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .build()

    private val detector = FaceDetection.getClient(options)

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val width = if (imageProxy.imageInfo.rotationDegrees % 180 == 0) imageProxy.width else imageProxy.height
            val height = if (imageProxy.imageInfo.rotationDegrees % 180 == 0) imageProxy.height else imageProxy.width

            detector.process(image)
                .addOnSuccessListener { faces ->
                    processFaces(faces, imageProxy)
                }
                .addOnCompleteListener { faces ->
                    onFaceDetected(faces.result ?: emptyList(), width, height)
                }
                .addOnFailureListener { e ->
                    Log.e("FaceAnalyzer", "Face detection failed", e)
                    onStatusChanged(FaceMonitorStatus.OUTSIDE_FRAME)
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun processFaces(faces: List<Face>, imageProxy: ImageProxy) {
        val width = if (imageProxy.imageInfo.rotationDegrees % 180 == 0) imageProxy.width else imageProxy.height
        val height = if (imageProxy.imageInfo.rotationDegrees % 180 == 0) imageProxy.height else imageProxy.width

        if (faces.isEmpty()) {
            livenessChecker.reset()
            onStatusChanged(FaceMonitorStatus.OUTSIDE_FRAME)
            imageProxy.close()
            return
        }

        if (faces.size > 1) {
            onStatusChanged(FaceMonitorStatus.MULTIPLE_FACES)
            imageProxy.close()
            return
        }

        val face = faces[0]
        val box = face.boundingBox

        // ROI Check (Registration: ~0.35, Verification: ~0.50)
        val centerX = box.centerX().toFloat() / width
        val centerY = box.centerY().toFloat() / height
        val faceWidthRatio = box.width().toFloat() / width

        val targetCenterY = if (isRegisterMode()) 0.35f else 0.50f
        
        // Balanced Centering Logic (Easier to capture)
        val isCentered = centerX in 0.42f..0.58f && centerY in (targetCenterY - 0.12f)..(targetCenterY + 0.12f)
        val isCorrectSize = faceWidthRatio in 0.30f..0.60f

        if (!isCentered || !isCorrectSize) {
            onStatusChanged(FaceMonitorStatus.NOT_CENTERED)
            imageProxy.close()
            return
        }
        
        // Pose Evaluation
        val poseViolation = evaluatePose(face)
        if (poseViolation != null) {
            onStatusChanged(poseViolation)
            imageProxy.close()
            return
        }

        // Occlusion Detection
        if (isFaceObscured(face)) {
            onStatusChanged(FaceMonitorStatus.FACE_OBSCURED)
            imageProxy.close()
            return
        }

        // Liveness Detection (Anti-Spoofing) - Only during Verification
        val bitmap = imageProxy.toBitmap()
        if (!isRegisterMode()) {
            if (!livenessChecker.isRealFace(bitmap, face, imageProxy.imageInfo.rotationDegrees)) {
                val rotatedFullFrame = rotateBitmap(bitmap, imageProxy.imageInfo.rotationDegrees)
                addCapturedFace(rotatedFullFrame) 
                onStatusChanged(FaceMonitorStatus.STATIC_IMAGE_DETECTED)
                imageProxy.close()
                return
            }
        }

        onStatusChanged(FaceMonitorStatus.INSIDE_FRAME)

        // Descriptor Generation
        val croppedFace = cropFace(bitmap, face.boundingBox, imageProxy.imageInfo.rotationDegrees)
        addCapturedFace(croppedFace)
        
        if (croppedFace != null) {
            val descriptor = faceNetProcessor.getFaceDescriptor(croppedFace)
            if (descriptor != null) {
                onDescriptorGenerated(descriptor)
            }
        }
        
        imageProxy.close()
    }

    private fun evaluatePose(face: Face): FaceMonitorStatus? {
        val yaw = face.headEulerAngleY // Yaw
        val pitch = face.headEulerAngleX // Pitch
        val target = getTargetPosition()
        
        return when {
            // Subject looks LEFT -> Positive Y
            yaw > 20f && target != FacePosition.LEFT -> FaceMonitorStatus.GAZE_LEFT
            // Subject looks RIGHT -> Negative Y
            yaw < -20f && target != FacePosition.RIGHT -> FaceMonitorStatus.GAZE_RIGHT
            // Subject looks UP (TOP) -> Positive X on this device
            pitch > 15f && target != FacePosition.TOP -> FaceMonitorStatus.GAZE_UP
            // Subject looks DOWN -> Negative X on this device
            pitch < -15f -> FaceMonitorStatus.GAZE_DOWN
            else -> null
        }
    }

    private fun isFaceObscured(face: Face): Boolean {
        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)
        val mouthBottom = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)
        val noseBase = face.getLandmark(FaceLandmark.NOSE_BASE)

        if (leftEye == null || rightEye == null || mouthBottom == null || noseBase == null) {
            return true
        }

        val leftEyePos = leftEye.position
        val rightEyePos = rightEye.position
        val mouthPos = mouthBottom.position
        
        val eyeMidpoint = PointF(
            (leftEyePos.x + rightEyePos.x) / 2f,
            (leftEyePos.y + rightEyePos.y) / 2f
        )
        
        val interEyeDistance = hypot(rightEyePos.x - leftEyePos.x, rightEyePos.y - leftEyePos.y)
        val eyeToMouthDistance = hypot(mouthPos.x - eyeMidpoint.x, mouthPos.y - eyeMidpoint.y)
        
        if (interEyeDistance > 0) {
            val ratio = eyeToMouthDistance / interEyeDistance
            if (ratio < 0.45f || ratio > 1.85f) {
                return true
            }
        }
        
        return false
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun cropFace(bitmap: Bitmap, boundingBox: Rect, rotationDegrees: Int): Bitmap? {
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        
        // Expand bounding box significantly (20%) to ensure whole face features are captured
        val widthScale = boundingBox.width() * 0.20f
        val heightScale = boundingBox.height() * 0.20f
        
        val left = (boundingBox.left - widthScale).toInt().coerceAtLeast(0)
        val top = (boundingBox.top - heightScale).toInt().coerceAtLeast(0)
        val right = (boundingBox.right + widthScale).toInt().coerceAtMost(rotatedBitmap.width)
        val bottom = (boundingBox.bottom + heightScale).toInt().coerceAtMost(rotatedBitmap.height)
        
        val width = right - left
        val height = bottom - top
        
        return if (width > 0 && height > 0) {
            Bitmap.createBitmap(rotatedBitmap, left, top, width, height)
        } else {
            null
        }
    }
}
