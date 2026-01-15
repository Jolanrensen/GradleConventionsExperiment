plugins {

    // creates tasks sync<ExampleProject>(Dev) and build<ExampleProject>(Dev)
    // and all encompassing tasks buildExampleFolders and syncExampleFolders
    alias(convention.plugins.buildExampleProjects)
    alias(convention.plugins.kotlinJvm)
}
