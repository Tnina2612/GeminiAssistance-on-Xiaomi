import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Mat khau ky KHONG nen nam trong repo. Thu tu uu tien:
//   1. keystore.properties (da gitignore)
//   2. bien moi truong GUARD_STORE_PASSWORD / GUARD_KEY_ALIAS / GUARD_KEY_PASSWORD
//   3. gia tri mac dinh cu (de ban dang cai van cap nhat duoc; hay doi khi co the)
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun signingValue(key: String, env: String, fallback: String): String =
    keystoreProps.getProperty(key)?.takeIf { it.isNotBlank() }
        ?: System.getenv(env)?.takeIf { it.isNotBlank() }
        ?: fallback

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
            storeFile = file(keystoreProps.getProperty("storeFile")?.takeIf { it.isNotBlank() } ?: "guard.keystore")
            storePassword = signingValue("storePassword", "GUARD_STORE_PASSWORD", "guardpass")
            keyAlias = signingValue("keyAlias", "GUARD_KEY_ALIAS", "guard")
            keyPassword = signingValue("keyPassword", "GUARD_KEY_PASSWORD", "guardpass")
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
