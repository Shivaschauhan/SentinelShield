plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val releaseStoreFile = providers.gradleProperty("RELEASE_STORE_FILE").orNull
val releaseStorePassword = providers.gradleProperty("RELEASE_STORE_PASSWORD").orNull
val releaseKeyAlias = providers.gradleProperty("RELEASE_KEY_ALIAS").orNull
val releaseKeyPassword = providers.gradleProperty("RELEASE_KEY_PASSWORD").orNull

val releaseSigningConfigured = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { !it.isNullOrBlank() }

val appVersionNameProp = providers.gradleProperty("VERSION_NAME").orNull?.trim()
val appVersionCodeProp = providers.gradleProperty("VERSION_CODE").orNull?.trim()?.toIntOrNull()

val parsedVersionName = appVersionNameProp?.removePrefix("v")?.removePrefix("V")?.takeIf { it.isNotEmpty() } ?: "0.1.0"
val parsedVersionCode = appVersionCodeProp ?: run {
    val semverParts = parsedVersionName.split('.').mapNotNull { part ->
        part.takeWhile { it.isDigit() }.toIntOrNull()
    }
    if (semverParts.isNotEmpty()) {
        val major = semverParts.getOrElse(0) { 0 }
        val minor = semverParts.getOrElse(1) { 0 }
        val patch = semverParts.getOrElse(2) { 0 }
        val calculated = major * 10000 + minor * 100 + patch
        if (calculated > 0) calculated else 1
    } else {
        1
    }
}

android {
    namespace = "com.sentinelshield.antitheft"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.sentinelshield.antitheft"
        minSdk = 26
        targetSdk = 36
        versionCode = parsedVersionCode
        versionName = parsedVersionName
    }

    signingConfigs {
        create("release") {
            if (releaseSigningConfigured) {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)
    implementation(libs.biometric)
    implementation(libs.fragment.ktx)
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.video)
    implementation(libs.camera.view)
    implementation("com.google.android.gms:play-services-wearable:18.1.0")
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.maps.android:maps-compose:4.4.1")
    testImplementation("junit:junit:4.13.2")
}
