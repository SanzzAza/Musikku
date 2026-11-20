plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.vyora.music"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.vyora.music"
        minSdk = 24
        targetSdk = 35
        // Di GitHub Actions nilai ini diisi otomatis dari nomor build
        versionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = System.getenv("VERSION_NAME") ?: "1.1.0"
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH")
            if (keystorePath != null && file(keystorePath).exists()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // Fase 1: R8 dinyalakan. Keep rules untuk Gson/NewPipe ada di proguard-rules.pro.
            isMinifyEnabled = true
            // Resource shrinking belum diaktifkan: beberapa resource dibaca lewat nama,
            // jadi aktifkan hanya setelah ada waktu menguji perilaku runtime.
            isShrinkResources = false
            val release = signingConfigs.getByName("release")
            // Pakai keystore rilis jika tersedia, kalau tidak pakai debug key (build lokal)
            signingConfig = if (release.storeFile != null) release else signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        // Wajib untuk NewPipe Extractor di Android < 13
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            // Hanya file lisensi/duplikat meta yang dibuang.
            // JANGAN buang META-INF/services: Rhino & MLKit memakai ServiceLoader.
            excludes += setOf(
                "META-INF/{AL2.0,LGPL2.1}",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
                "META-INF/INDEX.LIST"
            )
        }
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.navigation:navigation-compose:2.8.1")

    // Pemutar musik (ExoPlayer + MediaSession + Cache Offline untuk background & notifikasi)
    // 1.9.4 = rilis stabil terbaru yang masih kompatibel dengan compileSdk 35
    implementation("androidx.media3:media3-exoplayer:1.9.4")
    implementation("androidx.media3:media3-session:1.9.4")
    implementation("androidx.media3:media3-datasource:1.9.4")
    implementation("androidx.media3:media3-database:1.9.4")

    // Network
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.google.code.gson:gson:2.11.0")

    // Gambar
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Efek blur backdrop Liquid Glass (RenderEffect Android 12+, fallback ringan Android < 12)
    implementation("dev.chrisbanes.haze:haze:1.1.1")

    // Segmentasi subjek on-device untuk efek kedalaman "Kartu di Belakang Subjek" pada Poster Foto
    implementation("com.google.android.gms:play-services-mlkit-subject-segmentation:16.0.0-beta1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Audio full dari YouTube Music (v0.27.1 = rilis stabil terbaru)
    implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.27.1")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.2")
}
