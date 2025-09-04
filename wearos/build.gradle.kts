plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.safestride.wearos"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.safestride.wearos"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Wearable Services
    implementation(libs.play.services.wearable)

    // Compose BOM (Bill of Materials)
    implementation(platform(libs.androidx.compose.bom))

    // UI and Graphics
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)

    // Material Design Components
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.foundation)

    // Wear OS Tooling for Previews
    implementation(libs.androidx.wear.tooling.preview)

    // Activity for Compose
    implementation(libs.androidx.activity.compose)

    // SplashScreen API
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.play.services.location)
    implementation(libs.androidx.appcompat)

    // Testing Dependencies
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Debugging & Tooling
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Wear OS Material (for TimeText and other Wear components)
    implementation (libs.androidx.compose.material.v100)

    // Material 3 for Compose
    implementation (libs.androidx.material3)

    // Compose UI (required for general Compose functionality)
    implementation (libs.ui)

    // Kotlin Standard Library (required for Compose)
    implementation (libs.kotlin.stdlib)

    implementation("com.google.android.material:material:1.12.0")

    implementation("androidx.wear:wear-ongoing:1.0.0")

    implementation("androidx.wear:wear:1.3.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

}
