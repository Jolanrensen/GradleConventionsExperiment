package mybuild.conventionWithInput

import org.gradle.api.provider.ListProperty

abstract class ConventionWithInputExtension {

    abstract val inputList: ListProperty<String>

    companion object {
        const val NAME = "conventionWithInput"
    }
}