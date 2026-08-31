# Facial Verify Compose

A modern Android application built with Jetpack Compose that implements robust face registration and verification using on-device Machine Learning.

## 🚀 Features

- **Multi-Pose Face Registration**: Guided 4-step registration process (Straight, Left, Right, and Top) to capture a comprehensive facial profile.
- **On-Device Face Embeddings**: Uses the **MobileFaceNet** ONNX model via ONNX Runtime to generate 512-dimensional face descriptors for high-accuracy matching.
- **Real-Time Face Detection**: Integrated with **ML Kit Face Detection** for high-performance tracking and pose estimation (Yaw, Pitch).
- **Intelligent Auto-Capture**: Automatically triggers image capture and descriptor generation once the user reaches and holds the required head angle for 1 second.
- **Precise Region of Interest (ROI)**: Stricter alignment logic ensuring the face is centered and correctly sized within the UI oval before processing.
- **Live Verification**: Seamless comparison of live camera frames against all registered descriptors using Cosine Similarity.
- **Modern Architecture**: Built using **MVVM**, **Dagger Hilt**, and **Kotlin Coroutines/Flow** for state management.
- **Optimized Performance**: Small app footprint achieved through ABI filtering and R8 code/resource shrinking.

## 🛠️ Technology Stack

- **UI**: Jetpack Compose
- **Camera**: CameraX (Preview & ImageAnalysis)
- **Face Detection**: Google ML Kit
- **Inference Engine**: ONNX Runtime (Android)
- **ML Model**: `w600k_mbf.onnx` (MobileFaceNet)
- **Dependency Injection**: Dagger Hilt
- **Asynchronous Logic**: Kotlin Coroutines & StateFlow

## 📸 Screenshots

| Dashboard | Registration | Verification |
| :---: | :---: | :---: |
| <img src="https://github.com/user-attachments/assets/71225180-4e97-42a6-a55f-0283565c5108" width="250" /> | <img src="https://github.com/user-attachments/assets/df17897c-470d-4881-8c91-3d042ae44a3e" width="250" /> | <img src="https://github.com/user-attachments/assets/215ce6fe-8417-4f0b-bb13-05c1e0b85441" width="250" /> |

## 🏗️ Project Structure

- `camera/utils/`: Core logic for ONNX processing (`FaceNetProcessor`), ML Kit analysis (`FaceAnalyzer`), and mathematical operations (`VectorMath`).
- `camera/compose/`: Reusable UI components and the main camera screen logic.
- `verifyScreen/`: Dedicated view for displaying successful verification results.
- `chooseView/`: Navigation entry point for selecting Registration or Verification modes.

## ⚙️ Getting Started

1. Clone the repository.
2. Ensure you have the latest Android Studio installed.
3. Sync the project with Gradle.
4. Run the app on a physical device (recommended for camera performance).

> [!NOTE]
> For optimal results, ensure the environment is well-lit and position your face within the dashed oval during registration and verification.

## 📦 Optimization

The app is optimized for release:
- **Minification**: R8 enabled for code and resource shrinking.
- **ABI Filters**: Limited to `armeabi-v7a` and `arm64-v8a` to reduce APK size by removing emulator-specific native libraries.

---
Developed as a demonstration of high-performance, on-device biometric verification.
