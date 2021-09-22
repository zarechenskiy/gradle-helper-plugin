package com.github.zarechenskiy.gradlehelperplugin

import com.github.zarechenskiy.gradlehelperplugin.resolve.*
import com.intellij.openapi.project.Project
import com.intellij.psi.util.parentsOfType
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

fun List<KtCallExpression>.isModuleDependencyDeclaration(): Boolean {
    if (size != 3 ||
        get(0).calleeExpression?.text != PROJECT_WIDE_DEPENDENCY_DECLARATION_CALL ||
        get(1).calleeExpression?.text !in GRADLE_DEPENDENCY_TYPES ||
        get(2).calleeExpression?.text != GRADLE_DEPENDENCIES_BLOCK_NAME) {
        return false
    }
    return true
}

fun List<KtCallExpression>.isLibraryDependencyDeclaration(): Boolean {
    if (size != 2 ||
        get(0).calleeExpression?.text !in GRADLE_DEPENDENCY_TYPES ||
        get(1).calleeExpression?.text != GRADLE_DEPENDENCIES_BLOCK_NAME) {
        return false
    }
    return true
}

fun KtStringTemplateExpression.isModuleDependencyDeclaration() =
    parentsOfType<KtCallExpression>().toList().isModuleDependencyDeclaration()

fun String.withoutQuotes() =
    replace("\"", "")

fun Project.getModuleNamePrefix() =
    "$name."

fun String.toIntellijModuleName(): String =
    withoutQuotes()
        .dropWhile { it == ':' }
        .replace(GRADLE_MODULE_NAME_DELIMITER, INTELLIJ_MODULE_NAME_DELIMITER)
