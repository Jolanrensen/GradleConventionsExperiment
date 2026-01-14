pluginManagement {
    includeBuild("../build-settings-logic")
}

plugins {
    id("mysettings.version-catalog")
    id("mysettings.convention-catalog")
}

rootProject.name = "build-logic"

