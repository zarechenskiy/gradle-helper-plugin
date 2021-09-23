package com.github.zarechenskiy.gradlehelperplugin.inspections

import com.github.zarechenskiy.gradlehelperplugin.isLibraryDependencyDeclaration
import com.github.zarechenskiy.gradlehelperplugin.isModuleDependencyDeclaration
import com.github.zarechenskiy.gradlehelperplugin.withoutQuotes
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid

abstract class KtElementFinder : KtTreeVisitorVoid() {
    var foundElement: PsiElement? = null

    override fun visitKtElement(element: KtElement) {
        foundElement ?: super.visitKtElement(element)
    }

    fun clearSearchResult() {
        foundElement = null
    }
}

abstract class StringTemplateExpressionFinder(val string: String) : KtElementFinder() {
    override fun visitStringTemplateExpression(expression: KtStringTemplateExpression) {
        if (expression.text.withoutQuotes() == string && expression.isAppropriate()) {
            foundElement = expression
            return
        }
        super.visitStringTemplateExpression(expression)
    }

    abstract fun KtStringTemplateExpression.isAppropriate(): Boolean
}

class LibraryDependencyDeclarationFinder(libraryName: String) : StringTemplateExpressionFinder(libraryName) {
    override fun KtStringTemplateExpression.isAppropriate() =
        isLibraryDependencyDeclaration()
}

class ModuleDependencyDeclarationFinder(moduleName: String) : StringTemplateExpressionFinder(moduleName) {
    override fun KtStringTemplateExpression.isAppropriate() =
        isModuleDependencyDeclaration()
}
