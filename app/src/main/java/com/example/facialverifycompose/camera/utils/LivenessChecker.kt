package com.example.facialverifycompose.camera.utils

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import com.google.mlkit.vision.face.Face
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.Collections
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

class LivenessChecker(context: Context) {
    private val ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val ortSession: OrtSession?
    private var isClosed = false

    private val scoreHistory = mutableListOf<Float>()
    private val maxHistoryCount = 10
    private var consecutiveLowCount = 0
    private val consecutiveLowToFlagSpoof = 1
    private val realScoreThreshold = 0.45f
    private val targetScale = 2.7f

    init {
        ortSession = try {
            val modelBytes = context.assets.open("MiniFASNetV2.onnx").readBytes()
            ortEnv.createSession(modelBytes)
        } catch (e: Exception) {
            Log.e("LivenessChecker", "Failed to load MiniFASNetV2.onnx", e)
            null
        }
    }

    fun reset() {
        scoreHistory.clear()
        consecutiveLowCount = 0
    }

    fun isRealFace(
        bitmap: Bitmap,
        face: Face,
        rotationDegrees: Int
    ): Boolean {
        if (ortSession == null || isClosed) return true

        if (!isFaceQualityAcceptable(face, bitmap.width, bitmap.height)) {
            Log.d("LivenessChecker", "Liveness rejected: bad face quality")
            return false
        }

        val result = runInference(bitmap, face, rotationDegrees) ?: return false
        
        val realScore = result.first
        
        scoreHistory.add(realScore)
        if (scoreHistory.size > maxHistoryCount) {
            scoreHistory.removeAt(0)
        }

        val average = scoreHistory.average().toFloat()

        if (average <= realScoreThreshold) {
            consecutiveLowCount++
        } else {
            consecutiveLowCount = 0
        }

        val isLive = consecutiveLowCount < consecutiveLowToFlagSpoof

        Log.d("LivenessChecker", "LIVENESS: current real = $realScore, average real = $average, low count = $consecutiveLowCount, result = ${if (isLive) "LIVE" else "SPOOF"}")

        return isLive
    }

    private fun isFaceQualityAcceptable(face: Face, frameW: Int, frameH: Int): Boolean {
        val box = face.boundingBox
        val faceWidthPx = box.width()
        val minAcceptableWidth = 80.0f
        if (faceWidthPx < minAcceptableWidth) return false

        val margin = 0.02f
        val touchesEdge = box.left < frameW * margin ||
                box.top < frameH * margin ||
                box.right > frameW * (1.0f - margin) ||
                box.bottom > frameH * (1.0f - margin)
        
        if (touchesEdge) return false

        if (abs(face.headEulerAngleY) > 25f) return false
        if (abs(face.headEulerAngleX) > 20f) return false

        return true
    }

    private fun runInference(bitmap: Bitmap, face: Face, rotationDegrees: Int): Pair<Float, Float>? {
        val inputBuffer = preprocessFaceCrop(bitmap, face, rotationDegrees) ?: return null
        
        try {
            val inputName = ortSession?.inputNames?.iterator()?.next() ?: return null
            val inputTensor = OnnxTensor.createTensor(ortEnv, inputBuffer, longArrayOf(1, 3, 80, 80))
            
            inputTensor.use {
                val output = ortSession.run(Collections.singletonMap(inputName, inputTensor))
                output.use {
                    @Suppress("UNCHECKED_CAST")
                    val logits = (output.get(0).value as Array<FloatArray>)[0]
                    if (logits.size < 2) return null

                    // Softmax
                    val maxLogit = logits.maxOrNull() ?: 0f
                    val expValues = logits.map { exp((it - maxLogit).toDouble()) }
                    val sumExp = expValues.sum()
                    val probs = expValues.map { (it / sumExp).toFloat() }

                    val realProb = probs[1]
                    val spoofProb = probs[0]

                    return Pair(realProb, spoofProb)
                }
            }
        } catch (e: Exception) {
            Log.e("LivenessChecker", "Inference failed", e)
            return null
        }
    }

    private fun preprocessFaceCrop(bitmap: Bitmap, face: Face, rotationDegrees: Int): FloatBuffer? {
        val box = face.boundingBox
        
        // ML Kit bounding box is in pixels, relative to the rotated image if we passed rotation to InputImage.
        // However, in FaceAnalyzer, we get the bitmap from imageProxy.toBitmap() which might be unrotated YUV.
        // Wait, FaceAnalyzer.analyze calls InputImage.fromMediaImage(mediaImage, rotationDegrees).
        // The face.boundingBox is relative to that InputImage.
        
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        
        val srcW = rotatedBitmap.width.toFloat()
        val srcH = rotatedBitmap.height.toFloat()

        val boxX = box.left.toFloat()
        val boxY = box.top.toFloat()
        val boxW = box.width().toFloat()
        val boxH = box.height().toFloat()

        val maxFaceDim = max(boxW, boxH)
        val cropSize = maxFaceDim * targetScale

        val centerX = boxX + boxW / 2f
        val centerY = boxY + boxH / 2f

        var cropX = centerX - cropSize / 2f
        var cropY = centerY - cropSize / 2f

        cropX = max(0f, min(srcW - cropSize, cropX))
        cropY = max(0f, min(srcH - cropSize, cropY))

        val finalCropW = min(cropSize, srcW - cropX)
        val finalCropH = min(cropSize, srcH - cropY)

        if (finalCropW <= 0 || finalCropH <= 0) return null

        val croppedBitmap = Bitmap.createBitmap(rotatedBitmap, cropX.toInt(), cropY.toInt(), finalCropW.toInt(), finalCropH.toInt())
        val resizedBitmap = Bitmap.createScaledBitmap(croppedBitmap, 80, 80, true)

        val buffer = ByteBuffer.allocateDirect(1 * 3 * 80 * 80 * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        val intValues = IntArray(80 * 80)
        resizedBitmap.getPixels(intValues, 0, 80, 0, 0, 80, 80)

        // RGBA -> BGR Float Planar (CHW)
        val spatialArea = 80 * 80
        for (y in 0 until 80) {
            for (x in 0 until 80) {
                val pixel = intValues[y * 80 + x]
                val r = (pixel shr 16 and 0xFF).toFloat()
                val g = (pixel shr 8 and 0xFF).toFloat()
                val b = (pixel and 0xFF).toFloat()

                // MiniFASNet standard input: BGR planar
                val idx = y * 80 + x
                buffer.put(idx, b)
                buffer.put(spatialArea + idx, g)
                buffer.put(2 * spatialArea + idx, r)
            }
        }

        buffer.rewind()
        return buffer
    }

    fun close() {
        isClosed = true
        ortSession?.close()
        ortEnv.close()
    }
}
