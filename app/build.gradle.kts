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

// Version nam trong version.properties (on dinh, build lai ra cung so).
// Co the ghi de khi build: -PversionCode=... -PversionName=...
val versionProps = Properties().apply {
    val f = rootProject.file("version.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun versionValue(key: String, fallback: String): String =
    (project.findProperty(key) as String?)?.takeIf { it.isNotBlank() }
        ?: versionProps.getProperty(key)?.takeIf { it.isNotBlank() }
        ?: fallback

val appVersionCode = versionValue("versionCode", "30000001").toInt()
val appVersionName = versionValue("versionName", "4.1")

android {
    namespace = "com.gemini.guard"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.gemini.guard"
        minSdk = 26
        targetSdk = 34
        versionCode = appVersionCode
        versionName = appVersionName
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
        getByName("release") {
            // Cung chu ky voi ban debug de cai de len ban dang dung (khong mat quyen ADB).
            signingConfig = signingConfigs.getByName("fixed")
            isMinifyEnabled = false      // hidden API + reflection: bat minify de vo app
            isShrinkResources = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    lint {
        // CI chay lint de phat hien van de, nhung khong chan phat hanh APK.
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:6.1")
    testImplementation("junit:junit:4.13.2")
}

// Tien cho CI / script: in ra version dang dung.
tasks.register("printVersionName") {
    doLast { println(appVersionName) }
}
