plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "ru.rice.agent"
    compileSdk = 35
    defaultConfig {
        applicationId = "ru.rice.agent.offline"
        minSdk = 27
        targetSdk = 28
        versionCode = 4
        versionName = "0.2.0"
    }
    signingConfigs {
        create("riceDebug") {
            storeFile = rootProject.file("debug-signing/rice-debug.keystore")
            storePassword = "rice-debug-only"
            keyAlias = "rice"
            keyPassword = "rice-debug-only"
        }
    }
    buildTypes {
        getByName("debug") { signingConfig = signingConfigs.getByName("riceDebug") }
        getByName("release") { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
