plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "de.raphael.macro"
    compileSdk = 36

    defaultConfig {
        applicationId = "de.raphael.macro"
        minSdk = 28
        targetSdk = 35
        // Bei GitHub wird die Build-Nummer übergeben, damit jedes Update eine höhere Version hat
        val ci = (project.findProperty("ciVersionCode") as String?)?.toIntOrNull()
        versionCode = ci ?: 1
        versionName = "0.1." + (ci ?: 0)
    }

    // Gleicher Schlüssel wie bei den anderen Apps (Secret KEYSTORE_B64) → Updates ohne Neuinstallation
    signingConfigs {
        getByName("debug") {
            val ks = file("ci.keystore")
            if (ks.exists()) {
                storeFile = ks
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        // Release = nicht debuggable → Compose läuft deutlich flüssiger
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
}
