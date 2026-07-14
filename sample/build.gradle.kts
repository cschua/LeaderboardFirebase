import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.leaderboardkit.sample"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.leaderboardkit.sample"
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
    // The whole point of this module: everything a host app needs comes from
    // one dependency. No direct references to :leaderboard:domain/data/presentation/ui
    // anywhere below — that's what "public-api is the supported entry point" means in practice.
    implementation(project(":leaderboard:public-api"))

    // Anonymous sign-in for the demo — see SampleApplication.ensureSignedIn.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.datetime)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)

    debugImplementation(platform(libs.compose.bom))
    debugImplementation(libs.compose.ui.tooling)
}
