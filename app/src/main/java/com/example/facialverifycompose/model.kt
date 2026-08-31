package com.example.facialverifycompose

import android.graphics.Bitmap
import android.net.Uri

data class CapturedData(
    val facePosition: FacePosition? = null,
    var image: Bitmap? = null,
    var uri : Uri? = null,
    var descriptor : FloatArray? = null
)

enum class FacePosition {
    STRAIGHT,
    RIGHT,
    LEFT,
    TOP,
    BOTTOM
}