plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.dataframe)

    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Add general `dataframe` dependency
    implementation(libs.dataframe)
    // Add `kandy` dependency
    implementation(libs.kandy)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
