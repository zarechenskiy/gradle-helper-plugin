package com.github.zarechenskiy.gradlehelperplugin

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.parentsOfType
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

fun List<KtCallExpression>.isModuleDependencyDeclaration(): Boolean {
    if (size != 3 ||
        get(0).calleeExpression?.text != PROJECT_WIDE_DEPENDENCY_DECLARATION_CALL ||
        get(1).calleeExpression?.text !in DEPENDENCY_DECLARATION_LEVELS ||
        get(2).calleeExpression?.text != DEPENDENCIES_BLOCK_NAME
    ) {
        return false
    }
    return true
}

fun List<KtCallExpression>.isLibraryDependencyDeclaration(): Boolean {
    if (size != 2 ||
        get(0).calleeExpression?.text !in DEPENDENCY_DECLARATION_LEVELS ||
        get(1).calleeExpression?.text != DEPENDENCIES_BLOCK_NAME
    ) {
        return false
    }
    return true
}

fun PsiElement.isInsideBuildGradleKtsFile() =
    containingFile.name == BUILD_GRADLE_KTS_FILE_NAME

fun KtStringTemplateExpression.isModuleDependencyDeclaration() =
    parentsOfType<KtCallExpression>().toList().isModuleDependencyDeclaration()

fun KtStringTemplateExpression.isLibraryDependencyDeclaration() =
    parentsOfType<KtCallExpression>().toList().isLibraryDependencyDeclaration()

fun String.withoutQuotes() =
    replace("\"", "")

fun Project.getModuleNamePrefix() =
    "$name."

fun String.toIntellijModuleName(): String =
    withoutQuotes()
        .dropWhile { it == ':' }
        .replace(GRADLE_MODULE_NAME_DELIMITER, INTELLIJ_MODULE_NAME_DELIMITER)

fun Module.findScriptFile(): PsiFile? {
    val scriptFile = rootManager.contentRoots
        .map { it.findFileByRelativePath(BUILD_GRADLE_KTS_FILE_NAME) }
        .firstOrNull()
        ?: return null
    return PsiManager.getInstance(project).findFile(scriptFile)
}

fun Project.findModuleWithName(moduleName: String): Module? {
    return allModules().firstOrNull {
        it.name.substringAfter(getModuleNamePrefix()) == moduleName
    }
}

fun Module.getParentModuleName() =
    name.substringBefore(".$MAIN_CHILD_MODULE_NAME")
        .substringBefore(".$TEST_CHILD_MODULE_NAME")

fun PsiElement.isInsideDependenciesBlock() =
    parentsOfType<KtCallExpression>().lastOrNull()?.calleeExpression?.text == DEPENDENCIES_BLOCK_NAME

