import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.leaderboardkit.sampleretro"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.leaderboardkit.sampleretro"
        minSdk = 24
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            javaParameters.set(true)
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Unlike :sample (which depends only on :leaderboard:public-api), this module
    // wires :leaderboard:data/domain/presentation/ui directly — the facade's
    // documented "advanced usage" opt-out — so the app owns its own composition
    // root (RetroApplication) and drives LeaderboardViewModel's MVI contract
    // (state/intent/effect) itself instead of going through the public-api facade's screen().
    implementation(project(":leaderboard:domain"))
    implementation(project(":leaderboard:data"))
    implementation(project(":leaderboard:presentation"))
    implementation(project(":leaderboard:ui"))

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.datetime)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Not pulled in transitively from :leaderboard:ui — that module keeps Compose
    // `implementation`-only so it never leaks to consumers, so a host reaching for
    // the MVI contract directly (like this app) has to declare its own Compose deps.
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.animation.core)

    debugImplementation(platform(libs.compose.bom))
    debugImplementation(libs.compose.ui.tooling)
}
