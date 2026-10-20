import org.gradle.api.JavaVersion

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "za.co.farrusco.estellecherryq"
    compileSdk = 35

    defaultConfig {
        applicationId = "za.co.farrusco.estellecherryq"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "EF_1.2"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { compose = true }

    signingConfigs {
        create("permanentRelease") {
            val store = providers.environmentVariable("FARRUSCO_SIGNING_STORE_FILE").orNull
            if (store != null) {
                storeFile = file(store)
                storePassword = providers.environmentVariable("FARRUSCO_SIGNING_STORE_PASSWORD").orNull
                keyAlias = providers.environmentVariable("FARRUSCO_SIGNING_KEY_ALIAS").orNull
                keyPassword = providers.environmentVariable("FARRUSCO_SIGNING_KEY_PASSWORD").orNull
            }
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("permanentRelease")
            isMinifyEnabled = false
        }
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
