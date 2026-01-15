import mybuild.toCamelCaseByDelimiters
import org.gradle.kotlin.dsl.registering
import org.gradle.tooling.GradleConnector

plugins {
    alias(convention.plugins.kotlinJvm)
}

val syncExampleFolders by tasks.registering {
    group = "build"
    description = "Sync the versions in the nested Gradle build in ./projects"
}
val buildExampleFolders by tasks.registering {
    group = "verification"
    description = "Builds the nested Gradle build in ./projects to verify they compile correctly."
}

val promoteExamples by tasks.registering {
    group = "publishing"
    description = "Promotes the /projects/dev example projects to /projects"

    val projectsFolder = file("projects")

    doLast {
        // Deletes existing projects before promotion
        projectsFolder.listFiles()?.forEach {
            if (!it.isDirectory || it.name == "dev") return@forEach
            it.deleteRecursively()
        }
        logger.lifecycle("Removed example projects from /projects")
        projectsFolder.resolve("dev").listFiles()?.forEach {
            if (!it.isDirectory) return@forEach
            it.copyRecursively(projectsFolder.resolve(it.name))
        }
        logger.lifecycle("Copied example projects from /projects/dev to /projects")
    }
    finalizedBy(syncExampleFolders)
}

tasks.named("assemble") {
    dependsOn(syncExampleFolders)
}
tasks.named("check") {
    dependsOn(buildExampleFolders)
}

/**
 * Sets up example folder with sync and build tasks
 */
fun setupExampleFolder(folder: File, isDev: Boolean) {
    val name = folder.name.toCamelCaseByDelimiters().replaceFirstChar { it.uppercase() } +
            (if (isDev) "Dev" else "")

    val syncTask = setupSyncVersionsTask(name, folder, isDev)
    syncExampleFolders {
        dependsOn(syncTask)
    }

    val buildTask = setupBuildTask(name, folder)
    buildTask { dependsOn(syncTask) }
    buildExampleFolders {
        dependsOn(buildTask)
    }
}

/**
 * Registers task to build the example project.
 */
fun setupBuildTask(
    name: String,
    folder: File,
): TaskProvider<Task> =
    tasks.register("build$name") {
        group = "verification"
        description = "Builds the nested Gradle build in ./${folder.name}"
        doLast {
            GradleConnector.newConnector()
                .forProjectDirectory(folder)
                .connect()
                .use {
                    it.newBuild()
                        .forTasks("clean", "build")
                        .setStandardInput(System.`in`)
                        .setStandardOutput(System.out)
                        .setStandardError(System.err)
                        .run()
                }
        }
    }

/**
 * Registers task to sync and overwrite versions and settings for the example project.
 *
 * This includes:
 * - gradle-wrapper.properties
 * - gradle.properties
 * - libs.versions.toml
 * - settings.gradle.kts
 */
fun setupSyncVersionsTask(
    name: String,
    folder: File,
    isDev: Boolean,
    versionsToSync: List<String> = listOf("kotlin", "dataframe", "kandy")
): TaskProvider<Task> =
    tasks.register("sync$name") {
        group = "build"
        description = "Sync the versions in the nested Gradle build in ./${folder.name}"

        outputs.upToDateWhen { false }

        val libs = versionCatalogs.named("libs")
        val versions = versionsToSync.associateWith {
            libs.findVersion(it).get().requiredVersion
        }

        val sourceGradleWrapperProperties = file("gradle/wrapper/gradle-wrapper.properties")

        doLast {
            // overwrite gradle-wrapper.properties
            folder.resolve("gradle/wrapper/gradle-wrapper.properties").writeText(
                sourceGradleWrapperProperties.readText()
            )

            // overwrite gradle.properties
            folder.resolve("gradle.properties").writeText(
                """
                    kotlin.code.style=official
                    # Disabling incremental compilation will no longer be necessary
                    # when https://youtrack.jetbrains.com/issue/KT-66735 is resolved.
                    kotlin.incremental=false
                """.trimIndent()
            )

            // overwrite libs.versions.toml
            val libsVersionsToml = folder.resolve("gradle/libs.versions.toml")
            val versionRegex = """([a-zA-Z0-9-]+)\s*=\s*".+"""".toRegex()
            val newLibsVersionsTomlContent = libsVersionsToml.readText().lines().joinToString("\n") {
                val match = versionRegex.matchEntire(it) ?: return@joinToString it
                val name = match.groupValues[1]
                if (name !in versions) return@joinToString it

                """$name = "${versions[name]}""""
            }
            libsVersionsToml.writeText(newLibsVersionsTomlContent)

            // overwrite settings.gradle.kts

            // this can also be done by the --include-build argument,
            // however, writing it in the settings.gradle.kts file makes the IDE aware of the dependency substitution
            val generatedDevConfig =
                """
                // region generated-config
                
                // substitutes dependencies provided by the root project
                includeBuild("../../..")
                
                // endregion
                """.trimIndent()

            val regex = "// region generated-config(\\n|.)*?// endregion".toRegex()
            val settingsGradleKts = folder.resolve("settings.gradle.kts")
            val settingsGradleKtsContent = settingsGradleKts.readText()
            val newSettingsGradleKtsContent =
                when (regex) {
                    in settingsGradleKtsContent if isDev ->
                        settingsGradleKtsContent.replace(regex, generatedDevConfig)

                    !in settingsGradleKtsContent if isDev ->
                        settingsGradleKtsContent + "\n" + generatedDevConfig

                    in settingsGradleKtsContent if !isDev ->
                        settingsGradleKtsContent.replace(regex, "")

                    else ->
                        settingsGradleKtsContent

                }
            settingsGradleKts.writeText(newSettingsGradleKtsContent)
        }
    }

file("projects").listFiles()?.forEach {
    if (!it.isDirectory || it.name == "dev") return@forEach
    setupExampleFolder(it, false)
}
file("projects/dev").listFiles()?.forEach {
    if (!it.isDirectory) return@forEach
    setupExampleFolder(it, true)
}
