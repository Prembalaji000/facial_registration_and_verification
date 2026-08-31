package com.example.facialverifycompose.camera.utils

import androidx.compose.ui.graphics.Color

enum class FaceMonitorStatus {
    NO_CAMERA,
    OUTSIDE_FRAME,
    INSIDE_FRAME,
    SAME_PERSON,
    OTHER_PERSON,
    MULTIPLE_FACES,
    NOT_CENTERED,
    
    // Attention & Pose
    LOOKING_AWAY,
    GAZE_LEFT,
    GAZE_RIGHT,
    GAZE_UP,
    GAZE_DOWN,
    
    // Occlusion & Anti-Spoofing
    FACE_COVERED,
    HAND_GESTURE,
    STATIC_IMAGE_DETECTED,
    FACE_OBSCURED;

    val label: String
        get() = when (this) {
            NO_CAMERA -> "No Camera"
            OUTSIDE_FRAME -> "Outside Frame"
            INSIDE_FRAME -> "In Frame"
            SAME_PERSON -> "Verification Successful"
            OTHER_PERSON -> "Mismatch"
            MULTIPLE_FACES -> "Multiple Faces"
            NOT_CENTERED -> "Center your face in the oval"
            LOOKING_AWAY -> "Looking Away"
            GAZE_LEFT -> "Looking Left"
            GAZE_RIGHT -> "Looking Right"
            GAZE_UP -> "Looking Up"
            GAZE_DOWN -> "Looking Down"
            FACE_COVERED -> "Face Covered"
            HAND_GESTURE -> "Hand Detected"
            STATIC_IMAGE_DETECTED -> "Spoof Detected"
            FACE_OBSCURED -> "Face Obscured"
        }

    val color: Color
        get() = when (this) {
            NO_CAMERA -> Color.Gray
            OUTSIDE_FRAME, FACE_OBSCURED, NOT_CENTERED -> Color(0xFFFFA500) // Orange
            INSIDE_FRAME -> Color.Blue
            SAME_PERSON -> Color.Green
            OTHER_PERSON -> Color.Red
            MULTIPLE_FACES -> Color(0xFF800080) // Purple
            LOOKING_AWAY, GAZE_LEFT, GAZE_RIGHT, GAZE_UP, GAZE_DOWN -> Color.Yellow
            FACE_COVERED, HAND_GESTURE -> Color(0xFFFFA500) // Orange
            STATIC_IMAGE_DETECTED -> Color.Red
        }
}
