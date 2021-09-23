package com.github.zarechenskiy.gradlehelperplugin

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtCallExpression

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

fun String.withoutQuotes() =
    replace("\"", "")

fun Project.getModuleNamePrefix() =
    "$name."

fun String.toIntellijModuleName(): String =
    withoutQuotes()
        .dropWhile { it == ':' }
        .replace(GRADLE_MODULE_NAME_DELIMITER, INTELLIJ_MODULE_NAME_DELIMITER)
