pluginManagement {
    includeBuild("../build-settings-logic")
}

plugins {
    id("mysettings.base")
    id("mysettings.version-catalog")
    id("dev.panuszewski.typesafe-conventions") version "0.11.1"
}

rootProject.name = "build-logic"

