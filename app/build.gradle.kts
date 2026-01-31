plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.tfg_apli"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.tfg_apli"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += "-Xskip-metadata-version-check"
    }

    buildFeatures {
        compose = true
    }
    packagingOptions {
        resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // A veces los metadatos de kotlin chocan
            excludes += "META-INF/kotlin_module"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6") // Actualizado
    implementation(libs.androidx.activity.compose)

    // PLATAFORMA BOM (El cerebro de las versiones)
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Iconos extendidos (Asegúrate que la versión coincida o deja que BOM maneje la base)
    implementation("androidx.compose.material:material-icons-extended:1.7.2")

    // Retrofit & Gson
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
    implementation("com.google.firebase:firebase-auth-ktx")

    // Location
    implementation("com.google.android.gms:play-services-location:21.2.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.0") // Actualizado ligeramente

    // CameraX
    implementation("androidx.camera:camera-core:1.3.3")
    implementation("androidx.camera:camera-camera2:1.3.3")
    implementation("androidx.camera:camera-lifecycle:1.3.3")
    implementation("androidx.camera:camera-view:1.3.3")

    // ML Kit & Google Code Scanner
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")

    // Accompanist Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    // Charts & Coil

    implementation("io.coil-kt:coil-compose:2.6.0")

    // Material 2 (Corregido en TOML para ser de móvil)
    implementation(libs.androidx.compose.material)

    // Foundation (Ahora gestionado por BOM, adiós error)
    implementation(libs.androidx.compose.foundation)

    implementation(libs.androidx.biometric)
    implementation(libs.androidx.security.crypto)

    // QR
    implementation("com.google.zxing:core:3.5.2")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}