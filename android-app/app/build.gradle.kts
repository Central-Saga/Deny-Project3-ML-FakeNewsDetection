import java.io.File
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// ---------------------------------------------------------------------------
// Keystore Configuration for Release Builds
// ---------------------------------------------------------------------------
val keystorePropertiesFile: File? = listOf(
    rootProject.file("keystore.properties"),
    project.file("keystore.properties"),
    rootProject.file("android-app/keystore.properties"),
    rootProject.file("../keystore.properties")
).firstOrNull { it.exists() }

val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile != null && keystorePropertiesFile.exists()) {
        FileInputStream(keystorePropertiesFile).use { load(it) }
    }
}

val hasReleaseSigning: Boolean = keystorePropertiesFile != null &&
    !keystoreProperties.getProperty("storeFile").isNullOrBlank() &&
    !keystoreProperties.getProperty("storePassword").isNullOrBlank() &&
    !keystoreProperties.getProperty("keyAlias").isNullOrBlank() &&
    !keystoreProperties.getProperty("keyPassword").isNullOrBlank()

android {
    namespace = "com.example.fakenewsdetection"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.fakenewsdetection"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                val storePath = keystoreProperties.getProperty("storeFile")
                val resolvedStoreFile = if (File(storePath).isAbsolute) {
                    File(storePath)
                } else {
                    File(keystorePropertiesFile!!.parentFile, storePath)
                }
                storeFile = resolvedStoreFile
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    androidResources {
        // Hindari kompresi ganda pada artefak model biner/GZIP di dalam APK
        noCompress += listOf("gz", "onnx")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// Fail-fast bila assembleRelease dijalankan tanpa konfigurasi release keystore
gradle.taskGraph.whenReady {
    val releaseRequested = allTasks.any { task ->
        val name = task.name
        name.equals("assembleRelease", ignoreCase = true) ||
            name.equals("bundleRelease", ignoreCase = true) ||
            name.endsWith("Release", ignoreCase = false) && task.project.name == "app" && !name.contains("UnitTest") && !name.contains("AndroidTest") && !name.contains("lint", ignoreCase = true)
    }

    if (releaseRequested && !hasReleaseSigning) {
        throw GradleException(
            "\n[GAGAL] Konfigurasi rilis tidak lengkap!\n" +
            "Untuk melakukan build release, file 'keystore.properties' diperlukan agar APK ditandatangani.\n" +
            "Langkah penyelesaian:\n" +
            "  1. Salin 'keystore.properties.example' menjadi 'keystore.properties'.\n" +
            "  2. Buat keystore bila belum ada:\n" +
            "     keytool -genkeypair -v -keystore release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias release-key\n" +
            "  3. Isi storeFile, storePassword, keyAlias, dan keyPassword di 'keystore.properties'.\n" +
            "Catatan: Untuk build lokal pengembangan, gunakan './gradlew assembleDebug'.\n"
        )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // ONNX Runtime untuk inferensi model ML di Android
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.31.0")

    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
    testImplementation("com.microsoft.onnxruntime:onnxruntime:1.31.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
