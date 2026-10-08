plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

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
    namespace = "com.sentinelshield.antitheft.wear"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.sentinelshield.antitheft" // Must match mobile app for wearApp coupling
        minSdk = 30
        targetSdk = 33
        versionCode = parsedVersionCode
        versionName = parsedVersionName
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("com.google.android.gms:play-services-wearable:18.1.0")
    implementation("androidx.percentlayout:percentlayout:1.0.0")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    
    // Wear Compose
    val wearComposeVersion = "1.3.0"
    implementation("androidx.wear.compose:compose-material:$wearComposeVersion")
    implementation("androidx.wear.compose:compose-foundation:$wearComposeVersion")
    implementation("androidx.wear.compose:compose-navigation:$wearComposeVersion")
    
    // General Compose
    val composeBom = platform("androidx.compose:compose-bom:2023.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.8.2")
    
    // Wear OS UI components
    implementation("androidx.wear:wear:1.3.0")
}
