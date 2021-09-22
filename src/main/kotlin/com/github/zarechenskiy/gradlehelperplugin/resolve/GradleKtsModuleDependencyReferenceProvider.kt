package com.github.zarechenskiy.gradlehelperplugin.resolve

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.util.parentsOfType
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

class GradleKtsModuleDependencyReferenceProvider : PsiReferenceProvider() {
    companion object {
        private val MODULE_REGEX = "\"+:([^\"]+)\"+".toRegex()

        private fun getModuleName(text: String): String? {
            val groupValues = MODULE_REGEX.matchEntire(text)?.groupValues
            if (groupValues == null || groupValues.size != 2) {
                return null
            }
            return groupValues[1].replace(GRADLE_MODULE_NAME_DELIMITER, INTELLIJ_MODULE_NAME_DELIMITER)
        }
    }

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<out PsiReference> {
        if (element !is KtStringTemplateExpression || !element.isModuleDependencyDeclaration()) {
            return emptyArray()
        }
        val moduleName = getModuleName(element.text) ?: return emptyArray()
        return arrayOf(
            GradleKtsModuleReference(
                element,
                moduleName
            )
        )
    }
}

private fun KtStringTemplateExpression.isModuleDependencyDeclaration(): Boolean {
    val callParents = parentsOfType<KtCallExpression>().toList()
    if (callParents.size != 3 ||
        callParents[0].calleeExpression?.text != PROJECT_WIDE_DEPENDENCY_DECLARATION_CALL ||
        callParents[1].calleeExpression?.text !in GRADLE_DEPENDENCY_TYPES ||
        callParents[2].calleeExpression?.text != GRADLE_DEPENDENCIES_BLOCK_NAME) {
        return false
    }
    return true
}
