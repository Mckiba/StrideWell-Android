plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
}

val googleMapsStaticApiKey =
    (project.findProperty("GOOGLE_MAPS_STATIC_API_KEY") as String?) ?: "AIzaSyCPkTIFGiek4UUaEKDV-mVDgEBGRnys45s"

android {
    namespace = "com.stridewell"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.stridewell"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL",        "\"http://10.0.2.2:3000\"")
            buildConfigField("String", "STRAVA_CLIENT_ID",    "\"204378\"")
            buildConfigField("String", "STRAVA_REDIRECT_URI", "\"stridewell://localhost\"")
            // Web OAuth client ID from Google Cloud Console (Credentials → Web client)
            buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"72822339247-lvn41ic0uubh3ol1gr55gq923fusm9sj.apps.googleusercontent.com\"")
            // Apple Service ID registered in Apple Developer Console
            buildConfigField("String", "APPLE_CLIENT_ID",      "\"com.stridewell.service\"")
            // Backend relay receives Apple's form_post then redirects to this deep link
            buildConfigField("String", "APPLE_REDIRECT_URI",   "\"stridewell://oauth/apple/callback\"")
            buildConfigField("String", "GOOGLE_MAPS_STATIC_API_KEY", "\"AIzaSyCPkTIFGiek4UUaEKDV-mVDgEBGRnys45s\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "API_BASE_URL",        "\"https://stridewell-api-production.up.railway.app\"")
            buildConfigField("String", "STRAVA_CLIENT_ID",    "\"204378\"")
            buildConfigField("String", "STRAVA_REDIRECT_URI", "\"stridewell://localhost\"")
            buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"72822339247-lvn41ic0uubh3ol1gr55gq923fusm9sj.apps.googleusercontent.com\"")
            buildConfigField("String", "APPLE_CLIENT_ID",      "\"com.stridewell.service\"")
            buildConfigField("String", "APPLE_REDIRECT_URI",   "\"stridewell://oauth/apple/callback\"")
            buildConfigField("String", "GOOGLE_MAPS_STATIC_API_KEY", "\"AIzaSyCPkTIFGiek4UUaEKDV-mVDgEBGRnys45s\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose     = true
        buildConfig = true
    }
}

dependencies {
    // Core AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose BOM + UI
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // Retrofit + OkHttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // kotlinx.serialization
    implementation(libs.kotlinx.serialization.json)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Security (EncryptedSharedPreferences)
    implementation(libs.androidx.security.crypto)

    // Coil
    implementation(libs.coil.compose)

    // AndroidX Browser (Custom Tabs — Strava + Apple OAuth)
    implementation(libs.androidx.browser)

    // Credential Manager (Google Sign-In)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.play.services.location)

    // Splash Screen
    implementation(libs.androidx.core.splashscreen)

    // Firebase BOM + Messaging
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging.ktx)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
