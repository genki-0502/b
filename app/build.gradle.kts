/************************************
 *  app/build.gradle.kts  (Kotlin DSL)
 ************************************/
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")        // Kotlin 利用（KTX など）
    id("org.jetbrains.kotlin.kapt")           // ↓ Kotlin でアノテーション処理するなら
    id("androidx.navigation.safeargs")        // Navigation-SafeArgs(Java 用)
}

android {
    namespace       = "jp.ac.gifu_u.info.genki.jan"
    compileSdk      = 34                     // 手元の SDK に合わせて変更 OK

    defaultConfig {
        applicationId       = "jp.ac.gifu_u.info.genki.jan"
        minSdk              = 24
        targetSdk           = 34
        versionCode         = 1
        versionName         = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // DataBinding / ViewBinding
    buildFeatures {
        dataBinding = true
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17   // JDK 17 を使用
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {

    /* --- AndroidX 基本 --- */
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.13.0-alpha03")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0-alpha13")

    /* --- Navigation --- */
    implementation("androidx.navigation:navigation-fragment:2.7.7")
    implementation("androidx.navigation:navigation-ui:2.7.7")

    /* --- CameraX --- */
    val cameraX = "1.3.3"
    implementation("androidx.camera:camera-core:$cameraX")
    implementation("androidx.camera:camera-camera2:$cameraX")
    implementation("androidx.camera:camera-lifecycle:$cameraX")
    implementation("androidx.camera:camera-view:1.4.0-alpha03")

    /* --- ML Kit: Barcode Scanning --- */
    implementation("com.google.mlkit:barcode-scanning:17.2.0")

    /* --- Glide（画像表示）--- */
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0") // Java ファイル用に annotationProcessor で OK

    /* ---------- ここを追加 ---------- */

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.code.gson:gson:2.10.1")
    /* -------------------------------- */
}

