package com.github.zarechenskiy.gradlehelperplugin.services

import com.intellij.openapi.project.Project
import com.github.zarechenskiy.gradlehelperplugin.GradleHelperPluginBundle

class MyProjectService(project: Project) {

    init {
        println(GradleHelperPluginBundle.message("projectService", project.name))
    }
}
