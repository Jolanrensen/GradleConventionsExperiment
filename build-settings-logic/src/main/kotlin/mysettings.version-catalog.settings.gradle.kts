plugins {
    id("mysettings.base")
}

fun getRootDir(): File {
    var rootDir = settingsDir
    while (!rootDir.resolve("gradlew").exists()) {
        rootDir = rootDir.parentFile
    }
    return rootDir!!
}
/*
 * Base convention plugin for settings.gradle.kts files.
 * This makes sure all Gradle projects use the same version catalog.
 */
dependencyResolutionManagement {
    versionCatalogs {
        // so we can create a new 'libs' if it already exists
        defaultLibrariesExtensionName = "_default"
        create("libs") {
            try {
                from(
                    files(
                        getRootDir().resolve("gradle/libs.versions.toml").absolutePath,
                    ),
                )
            } catch (e: Exception) {
                logger.warn(
                    "Could not load version catalog (${getRootDir().absolutePath}/gradle/libs.versions.toml) from $settingsDir",
                    e
                )
            }
        }
    }
}
