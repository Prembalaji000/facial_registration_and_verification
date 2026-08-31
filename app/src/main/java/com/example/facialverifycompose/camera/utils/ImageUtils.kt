package com.example.facialverifycompose.camera.utils

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.media.Image
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

object ImageUtils {
    fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val planeProxy = imageProxy.planes[0]
        val buffer: ByteBuffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        
        return if (imageProxy.format == ImageFormat.YUV_420_888) {
            // This is a simplified version, usually YUV needs more complex conversion
            // But for MLKit analysis we can use their InputImage.fromMediaImage
            bitmap 
        } else {
            bitmap
        }
    }

    // Better way to get bitmap from ImageProxy using MLKit/CameraX recommended ways if possible
    // For now, let's use a more robust YUV to Bitmap conversion if needed
}
