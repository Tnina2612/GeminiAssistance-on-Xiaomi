plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.gemini.guard"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.gemini.guard"
        minSdk = 26
        targetSdk = 34
        // versionCode = so phut ke tu 1970 -> luon tang theo thoi gian build, dung o repo nao cung cai de len duoc
        versionCode = (System.currentTimeMillis() / 60000L).toInt()
        versionName = "4.0"
    }
    signingConfigs {
        create("fixed") {
            storeFile = file("guard.keystore")
            storePassword = "guardpass"
            keyAlias = "guard"
            keyPassword = "guardpass"
        }
    }
    buildTypes {
        getByName("debug") { signingConfig = signingConfigs.getByName("fixed") }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:6.1")
}
