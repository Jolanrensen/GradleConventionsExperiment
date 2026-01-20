pluginManagement {
    includeBuild("../build-settings-logic")
}

plugins {
    id("mysettings.base")
    id("mysettings.catalogs-inside-convention-plugins")
}

rootProject.name = "build-logic"

