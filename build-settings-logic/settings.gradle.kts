rootProject.name = "build-settings-logic"

dependencyResolutionManagement {
    // allows submodules to override repositories
    // Careful! Once you write `repositories {}`, the ones below are NOT included anymore
    repositoriesMode = RepositoriesMode.PREFER_PROJECT

    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

// versions should be kept in sync with `gradle/libs.versions.toml`
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}