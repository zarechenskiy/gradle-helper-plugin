package com.github.zarechenskiy.gradlehelperplugin.resolve

val GRADLE_DEPENDENCY_TYPES = listOf(
    "implementation",
    "testImplementation",
    "runtimeOnly",
    "compileOnly"
)

const val MAIN_SUBMODULE_NAME = "main"

const val TEST_SUBMODULE_NAME = "test"

const val PROJECT_WIDE_DEPENDENCY_DECLARATION_CALL = "project"

const val INTELLIJ_MODULE_NAME_DELIMITER = '.'

const val GRADLE_MODULE_NAME_DELIMITER = ':'

const val GRADLE_DEPENDENCIES_BLOCK_NAME = "dependencies"

const val BUILD_GRADLE_KTS_FILE_NAME = "build.gradle.kts"
