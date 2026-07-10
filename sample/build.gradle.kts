plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.leaderboardkit.sample"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.leaderboardkit.sample"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
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
