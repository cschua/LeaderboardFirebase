// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlinx.kover) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.detekt)
}

allprojects {
    apply(plugin = rootProject.libs.plugins.detekt.get().pluginId)

    detekt {
        toolVersion = rootProject.libs.versions.detekt.get()
        config.setFrom(files("${rootProject.projectDir}/config/detekt/detekt.yml"))
        buildUponDefaultConfig = true
        allRules = false
        baseline = file("${projectDir}/detekt-baseline.xml")
    }

    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        reports {
            html.required.set(true)
            xml.required.set(true)
            txt.required.set(false)
            sarif.required.set(true)
        }
    }
}

tasks.register("detektAll") {
    description = "Run detekt on all modules"
    group = "verification"
    dependsOn(allprojects.map { it.tasks.withType<io.gitlab.arturbosch.detekt.Detekt>() })
}

tasks.register("detektBaselineAll") {
    description = "Generate detekt baseline for all modules"
    group = "verification"
    dependsOn(allprojects.map { it.tasks.withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>() })
}
