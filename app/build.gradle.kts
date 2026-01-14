plugins {
    // Apply the shared build logic from a convention plugin.
    // The shared code is located in `build-logic/src/main/kotlin/mybuild.kotlinJvm.gradle.kts`.
    alias(convention.plugins.kotlinJvm)

    // Apply the Application plugin to add support for building an executable JVM application.
    application
}

dependencies {
    // Project "app" depends on project "utils". (Project paths are separated with ":", so ":utils" refers to the top-level "utils" project.)
    implementation(projects.utils)
}

application {
    // Define the Fully Qualified Name for the application main class
    // (Note that Kotlin compiles `App.kt` to a class with FQN `com.example.app.AppKt`.)
    mainClass = "org.example.app.AppKt"
}
