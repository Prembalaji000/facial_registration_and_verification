package com.example.facialverifycompose.camera.compose

import android.Manifest
import android.graphics.Bitmap
import android.util.Log
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.facialverifycompose.CapturedData
import com.example.facialverifycompose.FacePosition
import com.example.facialverifycompose.camera.utils.FaceAnalyzer
import com.example.facialverifycompose.camera.utils.FaceMonitorStatus
import com.example.facialverifycompose.camera.utils.FaceNetProcessor
import com.example.facialverifycompose.ml.FaceDetectionAnalyzer
import com.google.android.datatransport.runtime.ExecutionModule_ExecutorFactory.executor
import java.util.concurrent.Executors

@Composable
fun CameraScreen(
    onStatusChanged: (FaceMonitorStatus) -> Unit,
    onDescriptorGenerated: (FloatArray) -> Unit,
    setReferenceDescriptor: (FloatArray, Bitmap?) -> Unit,
    addCapturedFace: (Bitmap?) -> Unit,
    status: FaceMonitorStatus,
    matchScore: Float,
    faceDescriptor: FloatArray,
    cameraPermission: Boolean,
    storagePermission: Boolean,
    capturedFaces : List<CapturedData?>,
    faceBitmap: Bitmap?,
    capturedFace: CapturedData?,
    onSubmitClick: () -> Unit,
    onCloseClick: () -> Unit,
    isRegisterFace: Boolean
){
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { 
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                MATCH_PARENT,
                MATCH_PARENT
            )
        }
    }
    
    val faceNetProcessor = remember { FaceNetProcessor(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            faceNetProcessor.close()
        }
    }
    Log.e("edit 1", "${capturedFace?.facePosition}")

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var isCameraGranted by remember { mutableStateOf(cameraPermission) }
    var isStorageGranted by remember { mutableStateOf(storagePermission) }

    val isFace = remember { mutableStateOf(false) }
    val currentCapturedFace by rememberUpdatedState(capturedFace)
    val latestDescriptor by rememberUpdatedState(faceDescriptor)
    val latestBitmap by rememberUpdatedState(faceBitmap)

    LaunchedEffect(capturedFace) {
        isFace.value = false
    }

    LaunchedEffect(isFace.value, capturedFace, isRegisterFace) {
        if (isFace.value) {
            if (isRegisterFace && capturedFace == null) return@LaunchedEffect
            kotlinx.coroutines.delay(1000)
            if (isFace.value && latestDescriptor.isNotEmpty()) {
                setReferenceDescriptor(latestDescriptor, latestBitmap)
                isFace.value = false
            }
        }
    }
    
    val launcher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                isCameraGranted = true
                isStorageGranted = true
            }
        }

    LaunchedEffect(isCameraGranted) {
        if (isCameraGranted) {
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, FaceAnalyzer(
                            faceNetProcessor = faceNetProcessor,
                            onStatusChanged = { status ->
                                onStatusChanged(status)
                            },
                            onDescriptorGenerated = { descriptor ->
                                onDescriptorGenerated(descriptor)
                            },
                            addCapturedFace = {
                                addCapturedFace(it)
                            },
                            getTargetPosition = {
                                currentCapturedFace?.facePosition
                            },
                            isRegisterMode = {
                                isRegisterFace
                            },
                            onFaceDetected = { listFaces, frameWidth, frameHeight ->
                                if (listFaces.isNotEmpty()) {
                                    val face = listFaces[0]
                                    val box = face.boundingBox
                                    val centerX = box.centerX().toFloat() / frameWidth
                                    val centerY = box.centerY().toFloat() / frameHeight
                                    val faceWidthRatio = box.width().toFloat() / frameWidth
                                    
                                    val targetCenterY = if (isRegisterFace) 0.35f else 0.50f
                                    
                                    val isCentered = centerX in 0.42f..0.58f && 
                                                   centerY in (targetCenterY - 0.12f)..(targetCenterY + 0.12f)
                                    val isCorrectSize = faceWidthRatio in 0.30f..0.60f

                                    if (isCentered && isCorrectSize) {
                                        if (isRegisterFace) {
                                            isFace.value = when (currentCapturedFace?.facePosition) {
                                                FacePosition.LEFT -> face.headEulerAngleY > 20f
                                                FacePosition.RIGHT -> face.headEulerAngleY < -20f
                                                FacePosition.TOP -> face.headEulerAngleX > 15f
                                                FacePosition.STRAIGHT -> face.headEulerAngleX in -10f..10f && face.headEulerAngleY in -10f..10f
                                                else -> false
                                            }
                                        } else {
                                            isFace.value = face.headEulerAngleX in -10f..10f && face.headEulerAngleY in -10f..10f
                                        }
                                    } else {
                                        isFace.value = false
                                    }

                                } else {
                                    isFace.value = false
                                }
                            }
                        ))
                    }

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (exc: Exception) {
                    Log.e("CameraScreen", "Use case binding failed", exc)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }

    if (isCameraGranted){
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            AndroidView(
                factory = { context ->
                    FrameLayout(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            MATCH_PARENT,
                            MATCH_PARENT
                        )
                        addView(previewView)
                    }
                }
            )

            MainScreens(
                onTakePhotoClick = {
                    setReferenceDescriptor(faceDescriptor, faceBitmap)
                },
                storagePermission = isStorageGranted,
                status = status,
                matchScore = matchScore,
                capturedFaces = capturedFaces,
                isFaceDetected = isFace.value,
                onSubmitClick = onSubmitClick,
                onCloseClick = onCloseClick,
                isRegisterFace = isRegisterFace
            )
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ){
            Button(
                onClick = {
                    if (!isCameraGranted) {
                        launcher.launch(Manifest.permission.CAMERA)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black
                )
            ) {
                Text(
                    text = "OPEN CAMERA",
                    color = Color.White
                )
            }
        }
    }
}
