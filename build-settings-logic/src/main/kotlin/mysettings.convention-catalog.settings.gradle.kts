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
 * Creates a version catalog with convention plugins.
 */
dependencyResolutionManagement {
    versionCatalogs {
        // generate type-safe accessors for convention plugins
        create("convention") {
            val buildConventionFiles = getRootDir()
                .resolve("build-logic/src/main/kotlin").listFiles()!!

            for (it in buildConventionFiles) {
                if (!it.isFile || !it.name.endsWith(".gradle.kts")) continue

                val conventionName = it.name.removeSuffix(".gradle.kts")
                val aliasName = conventionName.removePrefix("mybuild.")
                plugin(aliasName, conventionName).version("")
            }
        }
    }
}
