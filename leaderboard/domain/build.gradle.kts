plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinx.kover)
}

// Pure Kotlin, no Android or Firebase dependencies allowed here by design —
// this module must stay usable outside of Android (e.g. in unit tests, KMP later).
kotlin {
    jvmToolchain(21)
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.datetime)
    api(libs.javax.inject)

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
                    "*_MembersInjector*"
                )
            }
        }
    }
}
