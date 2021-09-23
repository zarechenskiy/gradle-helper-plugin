package com.github.zarechenskiy.gradlehelperplugin.resolve

import com.github.zarechenskiy.gradlehelperplugin.*
import com.intellij.psi.PsiElement
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
                 it != project.name &&
                !it.endsWith(MAIN_CHILD_MODULE_NAME) &&
                !it.endsWith(TEST_CHILD_MODULE_NAME)
            }
            .map { ":${it.replace(INTELLIJ_MODULE_NAME_DELIMITER, GRADLE_MODULE_NAME_DELIMITER)}" }
            .toTypedArray()
    }
}
