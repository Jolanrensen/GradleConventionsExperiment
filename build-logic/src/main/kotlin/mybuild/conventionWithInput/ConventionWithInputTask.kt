package mybuild.conventionWithInput

import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

abstract class ConventionWithInputTask : DefaultTask() {

    @get:Input
    abstract val inputList: ListProperty<String>

    @TaskAction
    fun run() {
        logger.lifecycle("received ${inputList.get()}")
    }
}