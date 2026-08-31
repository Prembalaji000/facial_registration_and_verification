package com.example.facialverifycompose.camera.compose

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.facialverifycompose.CapturedData
import com.example.facialverifycompose.R
import com.example.facialverifycompose.camera.utils.FaceMonitorStatus

@Preview
@Composable
fun MainScreenPreview(){
    MainScreens(
        onTakePhotoClick = {},
        storagePermission = true,
        status = FaceMonitorStatus.OUTSIDE_FRAME,
        matchScore = 0f,
        capturedFaces = listOf(),
        isFaceDetected = false,
        onSubmitClick = {},
        onCloseClick = {},
        isRegisterFace = false
    )
}

@Composable
fun MainScreens(
    onTakePhotoClick: () -> Unit,
    storagePermission: Boolean,
    status: FaceMonitorStatus,
    matchScore: Float,
    capturedFaces : List<CapturedData?>,
    isFaceDetected : Boolean,
    onSubmitClick: () -> Unit,
    onCloseClick: () -> Unit,
    isRegisterFace: Boolean
) {
    val isCompleted = capturedFaces.isNotEmpty() && capturedFaces.all { it?.image != null }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        IconButton(
            onClick = onCloseClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(
                    top = 28.dp,
                    end = 28.dp
                )
                .size(48.dp)
                .background(
                    color = Color.White.copy(alpha = 0.9f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.Black,
                modifier = Modifier.size(24.dp)
            )
        }

        NewCameraScreen(
            onTakePhotoClick = onTakePhotoClick,
            storagePermission = storagePermission,
            status = status,
            matchScore = matchScore,
            isFaceDetected = isFaceDetected,
            isCompleted = isCompleted,
            isRegisterFace = isRegisterFace
        )
        if (isRegisterFace){
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding(),
            ) {
                BottomSheet(capturedFaces, onSubmitClick)
            }
        }
    }
}

@Preview
@Composable
fun CameraPreviews(){
    NewCameraScreen(
        onTakePhotoClick = {},
        storagePermission = true,
        status = FaceMonitorStatus.INSIDE_FRAME,
        matchScore = 0f,
        isFaceDetected = false,
        isCompleted = false,
        isRegisterFace = false,
    )
}

@Composable
fun NewCameraScreen(
    onTakePhotoClick: () -> Unit,
    storagePermission: Boolean,
    status: FaceMonitorStatus,
    matchScore: Float,
    isFaceDetected: Boolean,
    isCompleted: Boolean = false,
    isRegisterFace: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "")

    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Status Animation"
    )

    val isVerified =
        !isRegisterFace && status == FaceMonitorStatus.SAME_PERSON

    val displayText = when {
        isRegisterFace && isCompleted -> "Registration Completed"
        isRegisterFace -> status.label
        isVerified -> "Verification Successful"
        else -> status.label
    }

    val displayColor = when {
        isCompleted || isVerified -> Color(0xFF4CAF50)
        else -> status.color
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f)),
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(if (isRegisterFace) Alignment.TopCenter else Alignment.Center)
                .padding(
                    top = if (isRegisterFace) 72.dp else 0.dp,
                    start = 20.dp,
                    end = 20.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = if (isRegisterFace) {
                    "Face Registration"
                } else {
                    "Face Verification"
                },
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isRegisterFace) {
                    "Position your face inside the frame"
                } else {
                    "Look directly at the camera"
                },
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            DrawDashedOval(
                status = if (isCompleted || isVerified) {
                    FaceMonitorStatus.SAME_PERSON
                } else {
                    status
                },
                isRegisterFace = isRegisterFace
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = displayText,
                modifier = Modifier.alpha(
                    if (isCompleted || isVerified) 1f else alphaAnim
                ),
                fontSize = 19.sp,
                color = displayColor,
                fontWeight = FontWeight.Bold
            )

            if (
                !isRegisterFace &&
                (status == FaceMonitorStatus.SAME_PERSON ||
                        status == FaceMonitorStatus.OTHER_PERSON)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "Match Score  ${"%.1f".format(matchScore)}%",
                        modifier = Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 7.dp
                        ),
                        fontSize = 14.sp,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!isCompleted && !isRegisterFace) {
                CircleCard(
                    onTakePhotoClick = onTakePhotoClick,
                    storagePermission = storagePermission,
                    status = status,
                    isFaceDetected = isFaceDetected
                )
            }
        }
    }
}
@Composable
fun DrawDashedOval(status: FaceMonitorStatus, isRegisterFace: Boolean) {
    val ovalColor = status.color

    var left by remember { mutableFloatStateOf(0f) }
    var top by remember { mutableFloatStateOf(0f) }
    var right by remember { mutableFloatStateOf(0f) }
    var bottom by remember { mutableFloatStateOf(0f) }

    Canvas(modifier = Modifier
        .size(260.dp, 320.dp)
        .onGloballyPositioned { coordinates ->
            val position = coordinates.positionInRoot()
            val size = coordinates.size

            left = position.x
            top = position.y
            right = left + size.width
            bottom = top + size.height
        }) {
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        drawOval(
            color = ovalColor,
            size = Size(size.width, size.height),
            style = Stroke(width = 4f, pathEffect = dashEffect)
        )
        drawOval(
            color = Color.Transparent,
            topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
            size = Size(size.width - 8.dp.toPx(), size.height - 8.dp.toPx()),
            blendMode = BlendMode.Clear
        )
    }
}

@Composable
fun CircleCard(
    onTakePhotoClick: () -> Unit,
    storagePermission: Boolean,
    status: FaceMonitorStatus,
    isFaceDetected : Boolean,
) {
    //val isFaceDetected = status == FaceMonitorStatus.INSIDE_FRAME || status == FaceMonitorStatus.SAME_PERSON
    var isStorageGranted by remember { mutableStateOf(storagePermission) }
    val launcher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                isStorageGranted = true
            }
        }
    Card(
        shape = CircleShape,
        border = BorderStroke(1.5.dp, if(isFaceDetected)Color.White else Color.White.copy(alpha = 0.1f)),
        modifier = Modifier
            .padding(10.dp)
            .size(50.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        onClick = {
            if (!isStorageGranted){
                launcher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            onTakePhotoClick()
        },
        enabled = isFaceDetected,
    ) {}
}


@Composable
fun BottomSheet(capturedFaces : List<CapturedData?>, onSubmitClick: () -> Unit) {
    val listState = rememberLazyListState()

    LaunchedEffect(capturedFaces) {
        val lastCapturedIndex = capturedFaces.indexOfLast { it?.image != null }
        if (lastCapturedIndex != -1) {
            listState.animateScrollToItem(lastCapturedIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            LazyRow(state = listState) {
                items(capturedFaces) { data ->
                 BottomSheetItem(data)
                }
            }

            SubmitButton(
                isEnable = capturedFaces.all { it?.image != null },
                onSubmitClick = onSubmitClick
            )

        }
    }
}

@Composable
fun BottomSheetItem(
    capturedFaces : CapturedData?,
) {
    val color = if (capturedFaces?.image?.asImageBitmap() != null){
        colorResource(id = R.color.green)
    } else {
        colorResource(id = R.color.light_black)
    }
    println("BottomSheetItem : $capturedFaces")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(96.dp)
                .height(110.dp)
                .background(Color.LightGray, shape = RoundedCornerShape(10.dp))
                .border(
                    BorderStroke(1.dp, color),
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable {
                    //viewModel.onEvent(CameraScreenEvents.EditImage(capturedFaces))
                    Log.e("edit", "${capturedFaces?.facePosition}")
                },
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = capturedFaces?.image,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.8f))
                        .togetherWith(fadeOut(animationSpec = tween(500)) + scaleOut(targetScale = 0.8f))
                },
                label = "Image Transition"
            ) { image ->
                if (image != null) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            bitmap = image.asImageBitmap(),
                            contentDescription = "Captured Face",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(10.dp))
                                .graphicsLayer {
                                    scaleX = -1f
                                },
                            contentScale = ContentScale.Crop
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.green_tick),
                            contentDescription = null,
                            tint = colorResource(id = R.color.green),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = "Placeholder",
                        tint = Color.Black,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (capturedFaces?.facePosition != null){
            Text(
                modifier = Modifier,
                text = capturedFaces.facePosition.name.lowercase().replaceFirstChar { it.uppercase() },
                fontSize = 14.sp,
                color = Color.White,
            )
        }
    }
}

@Composable
fun SubmitButton(isEnable: Boolean, onSubmitClick: () -> Unit) {
    Button(
        shape = RoundedCornerShape(14.dp),
        onClick = onSubmitClick,
        enabled = isEnable,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF1E88E5),
            contentColor = Color.White,
            disabledContainerColor = Color.White.copy(alpha = 0.10f),
            disabledContentColor = Color.White.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .height(48.dp)
    ) {
        Text(text = "Submitted", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp,)
    }
}