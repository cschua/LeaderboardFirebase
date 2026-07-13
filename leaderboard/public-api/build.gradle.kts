import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlinx.kover)
}

android {
    namespace = "com.leaderboardkit"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
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

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    // Transitively brings :leaderboard:domain and :leaderboard:presentation's
    // public types (LeaderboardConfig, LeaderboardTheme, LeaderboardError, ...) —
    // the DSL/theme/config surface a host app legitimately needs alongside
    // LeaderboardKit itself.
    api(project(":leaderboard:ui"))

    // LeaderboardKit's own functions use @Composable/Modifier/LeaderboardTheme's
    // Compose types (ColorScheme, TextStyle, ...) directly in their signatures,
    // so these need to be `api`, not `implementation` — :leaderboard:ui declares
    // them implementation-only, which doesn't flow through to this module's consumers.
    api(platform(libs.compose.bom))
    api(libs.compose.runtime)
    api(libs.compose.ui)
    api(libs.compose.material3)
    api(libs.compose.animation.core)

    // Manual-wiring internals: FirestoreLeaderboardRepository and friends never
    // appear in LeaderboardKit's own public signatures, so this stays implementation-only.
    implementation(project(":leaderboard:data"))

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(project(":leaderboard:data"))
    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.truth)
}

kover {
    reports {
        filters {
            excludes {
                classes(
                    "*Factory*",
                    "*.BuildConfig",
                    "*.R",
                    "*.R$*"
                )
            }
        }
    }
}
