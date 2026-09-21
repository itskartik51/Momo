plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.personal.momo"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.personal.momo"
        minSdk = 24
        targetSdk = 34
        versionCode = 14
        versionName = "1.2405.14"

        resourceConfigurations += listOf("en", "hi")

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    signingConfigs {
        create("release") {
            val keystoreFile = file("../release.keystore")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = "MomoReleaseSecret2026"
                keyAlias = "momo"
                keyPassword = "MomoReleaseSecret2026"
            }
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("release")
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            isCrunchPngs = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }

    packaging {
        jniLibs {
            excludes += listOf(
                "**/libagora_lip_sync_extension.so",
                "**/libagora_spatial_audio_extension.so",
                "**/libagora_ai_noise_suppression_extension.so",
                "**/libagora_audio_beauty_extension.so"
            )
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.biometric:biometric:1.1.0")

    implementation("androidx.compose.ui:ui:1.7.0")
    implementation("androidx.compose.animation:animation:1.7.0")
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.0")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Firebase Platform, Firestore & Silent Authentication
    implementation(platform("com.google.firebase:firebase-bom:33.2.0"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")

    // Google Play Services Location for Proximity Tracking
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Coil Image Loader for Jetpack Compose
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Konfetti Celebration Particle System
    implementation("nl.dionsegijn:konfetti-compose:2.0.4")

    // Airbnb Lottie for Jetpack Compose
    implementation("com.airbnb.android:lottie-compose:6.4.0")

    // Agora Voice SDK for Realtime Calling
    implementation("io.agora.rtc:voice-sdk:4.3.1")
}
