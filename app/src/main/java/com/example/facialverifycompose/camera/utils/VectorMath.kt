package com.example.facialverifycompose.camera.utils

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object VectorMath {
    fun l2Normalize(vector: FloatArray) {
        if (vector.isEmpty()) return
        var sumSq = 0f
        for (value in vector) {
            sumSq += value * value
        }
        val norm = sqrt(max(sumSq, 1e-8f))
        for (i in vector.indices) {
            vector[i] /= norm
        }
    }

    fun computeCosineSimilarity(vectorA: FloatArray, vectorB: FloatArray): Float {
        if (vectorA.size != vectorB.size || vectorA.isEmpty()) return 0.0f
        
        var dotProduct = 0.0f
        var normA = 0.0f
        var normB = 0.0f
        
        for (i in vectorA.indices) {
            dotProduct += vectorA[i] * vectorB[i]
            normA += vectorA[i] * vectorA[i]
            normB += vectorB[i] * vectorB[i]
        }
        
        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator > 0) dotProduct / denominator else 0.0f
    }

    fun calculateMatchScorePercent(similarity: Float, masterReqSim: Float = 0.65f): Float {
        val prob: Float = if (similarity >= masterReqSim) {
            50.0f + ((similarity - masterReqSim) / (1.0f - masterReqSim)) * 50.0f
        } else {
            (similarity / masterReqSim) * 50.0f
        }
        return min(100.0f, max(0.0f, prob))
    }
}
