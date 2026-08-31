package com.example.facialverifycompose.camera.utils

import android.content.Context
import android.graphics.Bitmap
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class FaceNetProcessor(context: Context) {
    private val ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val ortSession: OrtSession
    private var isClosed = false

    init {
        try {
            val modelBytes = context.assets.open("w600k_mbf.onnx").readBytes()
            ortSession = ortEnv.createSession(modelBytes)
            Log.d("FaceNetProcessor", "ONNX Session created successfully")
        } catch (e: Exception) {
            Log.e("FaceNetProcessor", "Error loading ONNX model", e)
            throw e
        }
    }

    fun getFaceDescriptor(bitmap: Bitmap): FloatArray? {
        if (isClosed) return null
        
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 112, 112, true)
        val floatBuffer = bitmapToFloatBuffer(resizedBitmap)
        
        try {
            val inputName = ortSession.inputNames.iterator().next()
            val inputTensor = OnnxTensor.createTensor(ortEnv, floatBuffer, longArrayOf(1, 3, 112, 112))
            
            inputTensor.use {
                val output = ortSession.run(mapOf(inputName to inputTensor))
                output.use {
                    @Suppress("UNCHECKED_CAST")
                    val result = output.get(0).value as Array<FloatArray>
                    val embedding = result[0]
                    VectorMath.l2Normalize(embedding)
                    return embedding
                }
            }
        } catch (e: Exception) {
            Log.e("FaceNetProcessor", "Inference failed", e)
            return null
        }
    }

    private fun bitmapToFloatBuffer(bitmap: Bitmap): FloatBuffer {
        val buffer = ByteBuffer.allocateDirect(1 * 3 * 112 * 112 * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        val intValues = IntArray(112 * 112)
        bitmap.getPixels(intValues, 0, 112, 0, 0, 112, 112)

        // Swift normalization: (val - 127.5) / 127.5
        // NCHW format
        
        // R channel
        for (i in 0 until 112 * 112) {
            val pixel = intValues[i]
            buffer.put(i, ((pixel shr 16 and 0xFF) - 127.5f) / 127.5f)
        }
        // G channel
        for (i in 0 until 112 * 112) {
            val pixel = intValues[i]
            buffer.put(112 * 112 + i, ((pixel shr 8 and 0xFF) - 127.5f) / 127.5f)
        }
        // B channel
        for (i in 0 until 112 * 112) {
            val pixel = intValues[i]
            buffer.put(2 * 112 * 112 + i, ((pixel and 0xFF) - 127.5f) / 127.5f)
        }
        
        buffer.rewind()
        return buffer
    }

    fun close() {
        isClosed = true
        ortSession.close()
        ortEnv.close()
    }
}
