package com.github.zarechenskiy.gradlehelperplugin.resolve

import com.github.zarechenskiy.gradlehelperplugin.isModuleDependencyDeclaration
import com.github.zarechenskiy.gradlehelperplugin.toIntellijModuleName
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

class GradleKtsModuleDependencyReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<out PsiReference> {
        if (element !is KtStringTemplateExpression || !element.isModuleDependencyDeclaration()) {
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
