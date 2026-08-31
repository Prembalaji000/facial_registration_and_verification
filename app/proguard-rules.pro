# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/prem/Android/Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# For ONNX Runtime
-keep class ai.onnxruntime.** { *; }

# For ML Kit
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.vision.** { *; }

# For Dagger Hilt
-keep class dagger.hilt.** { *; }
