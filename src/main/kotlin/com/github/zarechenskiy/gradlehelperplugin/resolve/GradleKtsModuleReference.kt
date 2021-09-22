package com.github.zarechenskiy.gradlehelperplugin.resolve

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReferenceBase
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

class GradleKtsModuleReference(
    private val stringTemplate: KtStringTemplateExpression,
    private val moduleName: String
) : PsiReferenceBase<KtStringTemplateExpression>(stringTemplate, true) {
    override fun resolve(): PsiElement? {
        return stringTemplate.project
            .findModuleWithName(moduleName)
            ?.findScriptFile()
    }

    override fun getVariants(): Array<Any> {
        val project = stringTemplate.project
        return project.allModules()
            .map { it.name.substringAfter(project.getModuleNamePrefix()) }
            .filter {
                !it.endsWith(MAIN_SUBMODULE_NAME) &&
                !it.endsWith(TEST_SUBMODULE_NAME)
            }
            .map { ":${it.replace(INTELLIJ_MODULE_NAME_DELIMITER, GRADLE_MODULE_NAME_DELIMITER)}" }
            .toTypedArray()
    }
}

private fun Project.getModuleNamePrefix() = "$name."

private fun Project.findModuleWithName(moduleName: String): Module? {
    return allModules().firstOrNull {
        it.name.substringAfter(getModuleNamePrefix()) == moduleName
    }
}

private fun Module.findScriptFile(): PsiFile? {
    val scriptFile = rootManager.contentRoots
        .map { it.findFileByRelativePath(BUILD_GRADLE_KTS_FILE_NAME) }
        .firstOrNull()
        ?: return null
    return PsiManager.getInstance(project).findFile(scriptFile)
}
