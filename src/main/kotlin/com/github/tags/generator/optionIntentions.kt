package com.github.tags.generator

class KotlinOptions : AbstractOptionsGenerator("add kotlinOptions", "kotlinOptions { /*...*/ }")
class FreeCompilerArgs : SpecificOptionsGenerator("add freeCompiler", "freeCompilerArgs +=  ")
class JvmTargetArg : SpecificOptionsGenerator("add jvmTarget", "jvmTarget = '1.6' ")
class AllWarningsAsErrors : SpecificOptionsGenerator("warnings as errors", "allWarningsAsErrors = true")
class SetLanguageVersion : SpecificOptionsGenerator("set language version", "languageVersion = ")

