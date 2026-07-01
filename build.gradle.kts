plugins {

    // creates tasks sync<ExampleProject>(Dev) and build<ExampleProject>(Dev)
    // and all encompassing tasks buildExampleFolders and syncExampleFolders
    alias(conventions.plugins.mybuild.buildExampleProjects)
    alias(conventions.plugins.mybuild.kotlinJvm)
}
