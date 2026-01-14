import mybuild.toCamelCaseByDelimiters
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
    isDev: Boolean
): TaskProvider<Task> =
    tasks.register("sync$name") {
        group = "build"
        description = "Sync the versions in the nested Gradle build in ./${folder.name}"

        val kotlinVersion = libs.versions.kotlin.get()
        val dataframeVersion = libs.versions.dataframe.get()
        val kandyVersion = libs.versions.kandy.get()

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
            val libsVersionsToml = folder.resolve("libs.versions.toml")
            val newLibsVersionsTomlContent = libsVersionsToml.readText().lines().joinToString("\n") {
                when {
                    it.startsWith("kotlin =") -> """kotlin = "$kotlinVersion""""
                    it.startsWith("dataframe =") -> """dataframe = "$dataframeVersion""""
                    it.startsWith("kandy =") -> """kandy = "$kandyVersion""""
                    else -> it
                }
            }
            libsVersionsToml.writeText(newLibsVersionsTomlContent)

            // overwrite settings.gradle.kts
            val generatedConfig = buildString {
                appendLine("// region generated-config")
                val rootProjectName = folder.name + (if (isDev) "-dev" else "")
                appendLine("""rootProject.name = "$rootProjectName"""")
                if (isDev) {
                    appendLine(
                        """
                        includeBuild("../../..") {
                            dependencySubstitution {
                                substitute(module("org:example")).using(project(":utils"))
                            }
                        }
                        """.trimIndent()
                    )
                }
                appendLine("// endregion")
            }

            val regex = "// region generated-config(\\n|.)*?// endregion".toRegex()
            val settingsGradleKts = folder.resolve("settings.gradle.kts")
            val settingsGradleKtsContent = settingsGradleKts.readText()
            val newSettingsGradleKtsContent =
                when (regex) {
                    in settingsGradleKtsContent ->
                        settingsGradleKtsContent.replace(regex, generatedConfig)

                    !in settingsGradleKtsContent ->
                        """
                        $settingsGradleKtsContent
                        $generatedConfig
                        """.trimIndent()

                    else -> settingsGradleKtsContent
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