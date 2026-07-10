import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlinx.kover)
}

android {
    namespace = "com.leaderboardkit.presentation"
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

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    api(project(":leaderboard:domain"))

    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(project(":leaderboard:data"))
    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.truth)
}

kover {
    reports {
        filters {
            excludes {
                classes(
                    "*Module*",
                    "*Factory*",
                    "*_HiltModules*",
                    "*_Provide*",
                    "*_MembersInjector*",
                    "*.BuildConfig",
                    "*.R",
                    "*.R$*"
                )
            }
        }
    }
}
