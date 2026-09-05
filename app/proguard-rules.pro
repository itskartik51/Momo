# Jetpack Compose Runtime Rules
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
-dontwarn androidx.compose.**

# OkHttp & Coroutines Optimization Rules
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Biometric & Security Modules
-keep class androidx.biometric.** { *; }

# Keep Model Attributes & Reflection Metadata
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
