package com.example.facialverifycompose.ml

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

@SuppressLint("UnsafeOptInUsageError")
class FaceDetectionAnalyzer(
  private val onFaceDetected: (faces: MutableList<Face>, width: Int, height: Int, image : ImageProxy) -> Unit
) : ImageAnalysis.Analyzer {

  private val options = FaceDetectorOptions.Builder()
    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
    .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
    .enableTracking()
    .build()

  private val faceDetector = FaceDetection.getClient(options)

  override fun analyze(image: ImageProxy) {
    image.image?.let {
      val imageValue = InputImage.fromMediaImage(it, image.imageInfo.rotationDegrees)
      faceDetector.process(imageValue)
        .addOnCompleteListener { faces ->
           /* val faces = faces.result
            val filteredFaces = faces.filter { face ->
                face.headEulerAngleX in -5.0..5.0 && face.headEulerAngleY in -25.0..-10.0 }*/
          onFaceDetected(faces.result, image.width, image.height, image)
          image.image?.close()
          image.close()
        }
    }
  }
}

