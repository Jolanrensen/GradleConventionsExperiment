# gradleConventionsTest

This project uses [Gradle](https://gradle.org/).

It forms an experimental basis to be used in [Kotlin DataFrame](https://github.com/Kotlin/dataframe).

DataFrame is a big library with a lot of modules and examples that we wish to both test and share.
This repo contains my test project for relevant Gradle conventions.

We have two modules that represent a "project" like DataFrame:
* [`:app`](./app) - some main module that contains the application code.
* [`:utils`](./utils) - a module that contains some shared code that should be compiled and tested in a dev-example.

## Convention plugins

The entire project is built using [Composite Builds](https://docs.gradle.org/current/userguide/composite_builds.html)
and [Pre-compiled Script Plugins](https://docs.gradle.org/current/userguide/implementing_gradle_plugins_precompiled.html)
acting as [Convention Plugins](https://docs.gradle.org/current/userguide/implementing_gradle_plugins_convention.html).

In practice, this means we try to put all shared build logic for `build.gradle.kts` files
in the [`build-logic` directory](./build-logic) and all build logic for `settings.gradle.kts` files
in the [`build-settings-logic` directory](./build-settings-logic) (Following the example of [dokka](https://github.com/Kotlin/dokka)).

### For settings.gradle.kts files
At the moment, this project only has one "active" project, namely, the root project ":gradleConventionsTest".
However, DataFrame has multiple projects, that are included with `includeBuild` in the root `settings.gradle.kts`.

To sync settings and, most importantly, the version catalog, we can create Gradle settings files in
[the `build-settings-logic/src/main/kotlin` folder](./build-settings-logic/src/main/kotlin).
We can also use build-settings plugins from [`build-settings-logic`](./build-settings-logic) inside [`build-logic`](./build-logic)
if applied! We will use this to create type-safe convention plugin references inside [`build-logic`](./build-logic) scripts.

It's a Dokka convention to name the files something like `<prefix>settings.<name>.settings.gradle.kts`.

Check out [`mysettings.base.settings.gradle.kts`](./build-settings-logic/src/main/kotlin/mysettings.base.settings.gradle.kts); 
It contains everything we commonly put in a `settings.gradle.kts` file, like repositories and plugins.

Scripts can inherit each other, by using the `plugins { }` block (unfortunately, this cannot be done type safely).
Check out [`mysettings.version-catalog`](./build-settings-logic/src/main/kotlin/mysettings.version-catalog.settings.gradle.kts)
for an example. It builds on top of `mysettings.base`, but it also sets up the `libs` version catalog by searching for the top-level `libs.versions.toml` file.
This Convention Plugin can be applied to any `settings.gradle.kts` file in the project.

Similarly, I've created [`mysettings.convention-catalog`](./build-settings-logic/src/main/kotlin/mysettings.convention-catalog.settings.gradle.kts).
This plugin scans the [`build-logic` directory](./build-logic) for convention plugins and adds them to a version catalog called "convention".
This way, we can refer to convention plugins safely just like libraries!

Both scripts are joined together in [`mysettings.catalogs`](./build-settings-logic/src/main/kotlin/mysettings.catalogs.settings.gradle.kts)
which is actually applied to the root project.

Finally, we want to be able to use both version catalogs in [`build-logic`](./build-logic) as well for type safe access
of libraries and convention plugins.
The [Gradle documentation](https://docs.gradle.org/current/userguide/version_catalogs.html#sec:buildsrc-version-catalog) states this is not possible yet and recommends a workaround.
However, we can use the [Typesafe Conventions Gradle Plugin](https://github.com/radoslaw-panuszewski/typesafe-conventions-gradle-plugin)
for this purpose. It's used in a lot of projects on GitHub and is actively maintained.
We set it up in [`mysettings.catalogs-inside-convention-plugins`](./build-settings-logic/src/main/kotlin/mysettings.catalogs-inside-convention-plugins.settings.gradle.kts)
and use it in [`build-logic/settings.gradle.kts`](./build-logic/settings.gradle.kts).
Now we can also use the version catalogs in the `build-logic` directory :).

(One gotcha: any plugin you want to apply from a build-settings convention plugin must also be declared in the depenendencies of the outer
[`build-settings-logic/build.gradle.kts`](./build-settings-logic/build.gradle.kts) file, 
as well as (potentially non-applied) plugin in [`build-settings-logic/settings.gradle.kts`](./build-settings-logic/settings.gradle.kts)
otherwise Gradle cannot find it)

These build settings logic plugins can be applied in any `settings.gradle.kts` file like:
```kts
pluginManagement {
    includeBuild("path/to/build-settings-logic")
    ...
}

plugins {
    id("mysettings.<NAME_OF_PLUGIN>")
    ...
}
```

### For build.gradle.kts files
Now, to share dependencies and build logic between modules, we can create Gradle build scripts in
[the `build-logic/src/main/kotlin` folder](./build-logic/src/main/kotlin).

It's a Dokka convention to name the files something like `<prefix>build.<name>.gradle.kts`.

Check out [`mybuild.kotlinJvm`](./build-logic/src/main/kotlin/mybuild.kotlinJvm.gradle.kts).
This is an example plugin that sets up Kotlin JVM for any module applying it.
In contrast to the settings scripts, we can now access other build logic scripts safely, like
`plugins { alias(convention.plugins.base) }`, for instance.
We can also use normal Kotlin files to share code between scripts, like [`camelCase.kt`](./build-logic/src/main/kotlin/mybuild/camelCase.kt)

Convention plugins can also be created to "group" build logic together.
We wanted to set up build tasks for our examples and configure them correctly, however, polluting the main [`build.gradle.kts`](./build.gradle.kts) file
with all the configuration is not ideal.
Therefore, I created [`mybuild.buildExampleProjects`](./build-logic/src/main/kotlin/mybuild.buildExampleProjects.gradle.kts)
to contain it. See below for more explanation about how it works.

These build logic plugins can be applied in any project by adding this to the `settings.gradle.kts` file:
```kts
includeBuild("path/to/build-logic")
```
and in the `build.gradle.kts` file:
```kts
plugins {
    id("mybuild.<NAME_OF_PLUGIN>")
    ...
}
```

## The example projects

The example projects are located in the [`projects` directory](./projects).

The ones in the root are meant to be run as standalone projects by users.
They should be downloadable as zips and require no extra setup to run.

The ones in the [`dev` folder](./projects/dev) can also be run as standalone projects from the IDE, but
they are tested with the root project in a composite build, to catch breaking API changes.
In this example, we add and substitute the `:utils` module.

The idea is that the project in the `dev` folder is kept up to date with the root project,
so that we can catch breaking API changes early.
When we create a new release, we can copy the contents of the `dev` folder upwards by simply calling the
[`promoteExamples`](`./gradlew promoteExamples`) task.

See the Convention plugin [`mybuild.buildExampleProjects`](./build-logic/src/main/kotlin/mybuild.buildExampleProjects.gradle.kts).
This plugin is applied to the root project and creates the necessary tasks to interact with the examples.

It creates two sets of tasks:
* [`syncExampleFolders`](`./gradlew syncExampleFolders`) (and specific `sync-` tasks for each example project)
* [`buildExampleFolders`](`./gradlew buildExampleFolders`) (and specific `build-` tasks for each example project)

The `sync` tasks take care of overwriting Gradle setup file contents based on the root project.
This includes:
- gradle-wrapper.properties
- gradle.properties
- libs.versions.toml (syncing only the versions you specify)
- settings.gradle.kts (handling the `includeBuild` directive for the dev-example)

The `build` tasks use the Gradle Tooling API to `clean build` all example projects.

The tasks are linked to the main `assemble` and `check` tasks, respectively, so they are run automatically.
We will need to modify our GitHub Actions workflow to run the `syncExampleFolders` task on push to the master branch
and commit any changes made, to keep the examples up to date with the root project.

