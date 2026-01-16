import mybuild.conventionWithInput.ConventionWithInputExtension
import mybuild.conventionWithInput.ConventionWithInputTask

/*
 * Creating a convention plugin which requires input is best done
 * using an extension. This one demos that.
 */

plugins {
    alias(convention.plugins.base)
}

val conventionWithInputExtension = extensions.create<ConventionWithInputExtension>(ConventionWithInputExtension.NAME)

val taskWithInput by tasks.registering(ConventionWithInputTask::class) {
    inputList.convention(conventionWithInputExtension.inputList)
}

tasks.assemble {
    dependsOn(taskWithInput)
}