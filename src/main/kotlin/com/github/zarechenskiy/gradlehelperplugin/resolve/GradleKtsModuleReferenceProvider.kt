package com.github.zarechenskiy.gradlehelperplugin.resolve

import com.github.zarechenskiy.gradlehelperplugin.toIntellijModuleName
import com.github.zarechenskiy.gradlehelperplugin.withoutQuotes
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

class GradleKtsModuleReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<out PsiReference> {
        if (element !is KtStringTemplateExpression || !element.canBeProvidedWithReference()) {
            return emptyArray()
        }

        val moduleName = element.text.toIntellijModuleName()
        return arrayOf(
            GradleKtsModuleReference(
                element,
                moduleName
            )
        )
    }
}

private fun KtStringTemplateExpression.canBeProvidedWithReference() =
    text.withoutQuotes().startsWith(':')
