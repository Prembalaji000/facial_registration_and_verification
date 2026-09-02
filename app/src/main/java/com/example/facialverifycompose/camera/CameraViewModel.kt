package com.example.facialverifycompose.camera

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.facialverifycompose.CapturedData
import com.example.facialverifycompose.FacePosition
import com.example.facialverifycompose.camera.utils.FaceMonitorStatus
import com.example.facialverifycompose.camera.utils.VectorMath
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(): ViewModel() {

    private val listItem = listOf(
        CapturedData(facePosition = FacePosition.STRAIGHT),
        CapturedData(facePosition = FacePosition.LEFT),
        CapturedData(facePosition = FacePosition.RIGHT),
        CapturedData(facePosition = FacePosition.TOP),
    )
    private val viewModelState = MutableStateFlow(
        MyViewModelState(
            isLoading = false,
            capturedFace = listItem.find { it.image == null},
            cameraStateView = CameraViewState.PRIMARY_VIEW
        )
    )

    val uiState = viewModelState
        .map { it.uiState() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, viewModelState.value.uiState())

    private val _faceDescriptor = mutableStateOf<FloatArray?>(null)
    val faceDescriptor: State<FloatArray?> = _faceDescriptor

    private val _faceBitmap = mutableStateOf<Bitmap?>(null)
    val faceBitmap: State<Bitmap?> = _faceBitmap
    
    private val _referenceDescriptor = mutableStateOf<FloatArray?>(null)
    
    private val frameBuffer = mutableListOf<FloatArray>()
    private val maxBufferCount = 10


    init {
        viewModelState.update { it.copy(capturedFaces = listItem) }
    }

    fun addCapturedFace(image: Bitmap?, descriptor: FloatArray?) {
        val currentFace = viewModelState.value.capturedFace

        viewModelState.update { state ->

            val updatedList = state.capturedFaces.map { item ->
                if (item?.facePosition == currentFace?.facePosition) {
                    item?.copy(image = image, descriptor = descriptor)
                } else {
                    item
                }
            }

            val nextFace = updatedList.firstOrNull { it?.image == null }

            state.copy(
                capturedFace = nextFace,
                capturedFaces = updatedList,
                lastUpdate = System.currentTimeMillis()
            )
        }
    }

    fun onStatusChanged(newStatus: FaceMonitorStatus) {
        val state = viewModelState.value
        viewModelState.update { currentState ->
            // In Verify mode, if we are already matched, don't let INSIDE_FRAME flicker it back to blue
            if (currentState.cameraStateView == CameraViewState.VERIFY_VIEW && 
                (currentState.status == FaceMonitorStatus.SAME_PERSON || currentState.status == FaceMonitorStatus.STATIC_IMAGE_DETECTED) && 
                newStatus == FaceMonitorStatus.INSIDE_FRAME) {
                currentState
            } else {
                currentState.copy(status = newStatus)
            }
        }
        
        if (newStatus != FaceMonitorStatus.INSIDE_FRAME && 
            newStatus != FaceMonitorStatus.SAME_PERSON && 
            newStatus != FaceMonitorStatus.OTHER_PERSON &&
            newStatus != FaceMonitorStatus.STATIC_IMAGE_DETECTED) {
            frameBuffer.clear()
            viewModelState.update { it.copy(matchScore = 0f) }
        }

        // If spoof detected in Verify mode, navigate to success screen with failure status
        if (state.cameraStateView == CameraViewState.VERIFY_VIEW && newStatus == FaceMonitorStatus.STATIC_IMAGE_DETECTED) {
            viewModelState.update { 
                it.copy(
                    cameraStateView = CameraViewState.SUCCESS_VIEW,
                    verifiedBitmap = _faceBitmap.value,
                    isSpoofDetected = true
                )
            }
        }
    }

    fun onDescriptorGenerated(descriptor: FloatArray) {
        val normalized = descriptor.copyOf()
        VectorMath.l2Normalize(normalized)
        
        frameBuffer.add(normalized)
        if (frameBuffer.size > maxBufferCount) {
            frameBuffer.removeAt(0)
        }
        
        val smoothed = averageDescriptors(frameBuffer)
        VectorMath.l2Normalize(smoothed)
        _faceDescriptor.value = smoothed
        
        verifyFace(smoothed)
    }

    fun setFaceBitmap(bitmap: Bitmap?){
        _faceBitmap.value = bitmap
    }

    private fun verifyFace(currentDescriptor: FloatArray) {
        val state = viewModelState.value
        // Slightly lower threshold for better UX, with a high-confidence "verified" state
        val lowThreshold = 0.35f 
        val targetThreshold = 0.45f

        if (state.cameraStateView == CameraViewState.VERIFY_VIEW) {
            val registeredDescriptors = state.capturedFaces.mapNotNull { it?.descriptor }
            
            if (registeredDescriptors.isEmpty()) {
                viewModelState.update { it.copy(status = FaceMonitorStatus.OUTSIDE_FRAME, matchScore = 0f) }
                return
            }

            var maxSimilarity = -1f
            for (regDesc in registeredDescriptors) {
                val sim = VectorMath.computeCosineSimilarity(regDesc, currentDescriptor)
                if (sim > maxSimilarity) {
                    maxSimilarity = sim
                }
            }

            // Using the targetThreshold for score calculation to be more accurate
            val score = VectorMath.calculateMatchScorePercent(maxSimilarity, targetThreshold)

            viewModelState.update {
                it.copy(
                    matchScore = score,
                    status = if (maxSimilarity >= lowThreshold) FaceMonitorStatus.SAME_PERSON else FaceMonitorStatus.OTHER_PERSON,
                    cameraStateView = if (maxSimilarity >= targetThreshold) CameraViewState.SUCCESS_VIEW else it.cameraStateView,
                    verifiedBitmap = if (maxSimilarity >= targetThreshold) _faceBitmap.value else it.verifiedBitmap
                )
            }
        } else {
            viewModelState.update {
                it.copy(matchScore = 0f)
            }
        }
    }

    private fun averageDescriptors(descriptors: List<FloatArray>): FloatArray {
        if (descriptors.isEmpty()) return FloatArray(0)
        val size = descriptors[0].size
        val averaged = FloatArray(size)
        
        for (vec in descriptors) {
            for (i in 0 until size) {
                averaged[i] += vec[i]
            }
        }
        
        for (i in 0 until size) {
            averaged[i] /= descriptors.size.toFloat()
        }
        
        return averaged
    }
    
    fun setReferenceDescriptor(descriptor: FloatArray) {
        println("descriptor : ${descriptor.contentToString()}")
        _referenceDescriptor.value = descriptor
    }

    fun switchViewState(cameraStateView: CameraViewState){
        viewModelState.update { 
            it.copy(
                cameraStateView = cameraStateView,
                status = FaceMonitorStatus.OUTSIDE_FRAME,
                matchScore = 0f,
                isSpoofDetected = false
            ) 
        }
    }

}

data class MyViewModelState(
    val isLoading: Boolean? = false,
    val status: FaceMonitorStatus? = FaceMonitorStatus.OUTSIDE_FRAME,
    val matchScore: Float? = null,
    val lastUpdate : Long = System.currentTimeMillis(),
    val faceDetection : Boolean = false,
    val capturedFace : CapturedData? = null,
    val capturedFaces : List<CapturedData?> = listOf(),
    val cameraStateView: CameraViewState? = CameraViewState.PRIMARY_VIEW,
    val verifiedBitmap: Bitmap? = null,
    val isSpoofDetected: Boolean = false
){
    fun uiState() = MyUiState(
        isLoading = isLoading,
        status = status,
        matchScore = matchScore,
        lastUpdate = lastUpdate,
        faceDetection = faceDetection,
        capturedFace = capturedFace,
        capturedFaces = capturedFaces,
        cameraStateView = cameraStateView,
        verifiedBitmap = verifiedBitmap,
        isSpoofDetected = isSpoofDetected
    )
}

data class MyUiState(
    val isLoading: Boolean?,
    val status: FaceMonitorStatus?,
    val matchScore: Float?,
    val lastUpdate : Long,
    val faceDetection : Boolean,
    val capturedFace : CapturedData?,
    val capturedFaces : List<CapturedData?>,
    val cameraStateView: CameraViewState?,
    val verifiedBitmap: Bitmap?,
    val isSpoofDetected: Boolean
)

enum class CameraViewState(val value: String){
    PRIMARY_VIEW("primary_view"),
    REGISTER_VIEW("register_view"),
    VERIFY_VIEW("verify_view"),
    SUCCESS_VIEW("success_view")
}
