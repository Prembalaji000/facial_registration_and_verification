package com.example.facialverifycompose.camera.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.facialverifycompose.camera.CameraViewModel
import com.example.facialverifycompose.camera.CameraViewState
import com.example.facialverifycompose.camera.utils.FaceMonitorStatus
import com.example.facialverifycompose.chooseVIew.ChooseViewScreen
import com.example.facialverifycompose.verifyScreen.VerifyScreen

@Composable
fun CameraScreenRouter(
    viewModel: CameraViewModel = hiltViewModel(),
    cameraPermission: Boolean,
    storagePermission: Boolean
){
    val uiState by viewModel.uiState.collectAsState()

    when (uiState.cameraStateView){
        CameraViewState.PRIMARY_VIEW -> {
            ChooseViewScreen(
                onViewClick = { cameraStateView ->
                    viewModel.switchViewState(cameraStateView)
                }
            )
        }

        CameraViewState.REGISTER_VIEW -> {
            CameraScreen(
                cameraPermission = cameraPermission,
                storagePermission = storagePermission,
                status = uiState.status?: FaceMonitorStatus.OUTSIDE_FRAME,
                matchScore = uiState.matchScore?: 0f,
                faceDescriptor = viewModel.faceDescriptor.value?: FloatArray(0),
                onStatusChanged = {
                    viewModel.onStatusChanged(it)
                },
                onDescriptorGenerated = {
                    viewModel.onDescriptorGenerated(it)
                },
                setReferenceDescriptor = { descriptor, bitmap ->
                    viewModel.setReferenceDescriptor(descriptor)
                    if (uiState.cameraStateView == CameraViewState.REGISTER_VIEW) {
                        viewModel.addCapturedFace(bitmap, descriptor)
                    }
                },
                addCapturedFace = { viewModel.setFaceBitmap(it) },
                capturedFaces = uiState.capturedFaces,
                faceBitmap = viewModel.faceBitmap.value,
                capturedFace = uiState.capturedFace,
                onSubmitClick = {
                    viewModel.switchViewState(CameraViewState.PRIMARY_VIEW)
                },
                onCloseClick = {
                    viewModel.switchViewState(CameraViewState.PRIMARY_VIEW)
                },
                isRegisterFace = true
            )
        }

        CameraViewState.VERIFY_VIEW -> {
            CameraScreen(
                cameraPermission = cameraPermission,
                storagePermission = storagePermission,
                status = uiState.status?: FaceMonitorStatus.OUTSIDE_FRAME,
                matchScore = uiState.matchScore?: 0f,
                faceDescriptor = viewModel.faceDescriptor.value?: FloatArray(0),
                onStatusChanged = {
                    viewModel.onStatusChanged(it)
                },
                onDescriptorGenerated = {
                    viewModel.onDescriptorGenerated(it)
                },
                setReferenceDescriptor = { descriptor, bitmap ->
                    viewModel.setReferenceDescriptor(descriptor)
                    if (uiState.cameraStateView == CameraViewState.REGISTER_VIEW) {
                        viewModel.addCapturedFace(bitmap, descriptor)
                    }
                },
                addCapturedFace = { viewModel.setFaceBitmap(it) },
                capturedFaces = uiState.capturedFaces,
                faceBitmap = viewModel.faceBitmap.value,
                capturedFace = uiState.capturedFace,
                onSubmitClick = {
                    viewModel.switchViewState(CameraViewState.PRIMARY_VIEW)
                },
                onCloseClick = {
                    viewModel.switchViewState(CameraViewState.PRIMARY_VIEW)
                },
                isRegisterFace = false
            )
        }

        CameraViewState.SUCCESS_VIEW -> {
            VerifyScreen(
                verifiedBitmap = uiState.verifiedBitmap,
                isSpoofDetected = uiState.isSpoofDetected,
                onDoneClick = {
                    viewModel.switchViewState(CameraViewState.PRIMARY_VIEW)
                }
            )
        }
        else -> {}
    }
}