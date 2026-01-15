plugins {
    // Apply the shared build logic from a convention plugin.
    // The shared code is located in `build-logic/src/main/kotlin/mybuild.kotlinJvm.gradle.kts`.
    alias(convention.plugins.kotlinJvm)
    // Apply Kotlin Serialization plugin from `gradle/libs.versions.toml`.
    alias(libs.plugins.kotlinPluginSerialization)
}

dependencies {
    // Apply the kotlinx bundle of dependencies from the version catalog (`gradle/libs.versions.toml`).
    implementation(libs.bundles.kotlinxEcosystem)
    testImplementation(kotlin("test"))
}

group = "org.example"
