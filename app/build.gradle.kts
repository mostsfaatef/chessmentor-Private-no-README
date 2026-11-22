plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // Room uses kapt here: the version catalog has no KSP plugin entry,
    // so kapt is the documented fallback (see README §5/§7). If you add
    // KSP to the catalog later, swap this line for `id("com.google.devtools.ksp")`
    // and change `kapt(...)` below to `ksp(...)`.
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.chessmentor"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.chessmentor"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0-phase4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Phase 2: Stockfish native lib — NDK is required for this block.
        // Without an NDK installed, comment out externalNativeBuild and the
        // app still builds; StockfishEngine then reports "not built yet".
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
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
    }

    buildFeatures {
        compose = true
    }

    // Phase 2: builds app/src/main/cpp/CMakeLists.txt.
    // CMake builds a stub lib when STOCKFISH_DIR is not set (see that file),
    // so a first build without Stockfish sources still succeeds.
    // Pass -DSTOCKFISH_DIR=/path/to/Stockfish to build the real engine.
    externalNativeBuild {
        cmake { path = file("src/main/cpp/CMakeLists.txt"); version = "3.22.1" }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling.preview)

    // Phase 3 — Room (progress / lessons / puzzles / games / achievements).
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // Phase 4 notifications use AlarmManager + BroadcastReceiver only —
    // no WorkManager dependency is added, so the Phase 1 build cannot
    // break on it. See notifications/DailyPuzzleScheduler.kt and README §9.
}
