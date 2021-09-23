package com.github.zarechenskiy.gradlehelperplugin.inspections

import com.github.zarechenskiy.gradlehelperplugin.*
import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderEntry
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentsOfType
import org.jetbrains.kotlin.idea.util.projectStructure.module
import org.jetbrains.kotlin.psi.*

class GradleKtsRedundantDependencyInspection : LocalInspectionTool(), CleanupLocalInspectionTool {
    companion object {
        private val DESCRIPTION_TEMPLATE = GradleHelperPluginBundle.message("inspection.redundant.dependency.problem.descriptor")
        private val QUICK_FIX_NAME = GradleHelperPluginBundle.message("inspection.delete.dependency.quickfix")
    }

    private val deleteDependencyQuickFix = DeleteDependencyQuickFix()

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        GradleKtsRedundantDependencyVisitor(holder)

    inner class GradleKtsRedundantDependencyVisitor(private val holder: ProblemsHolder) : KtVisitorVoid() {
        private lateinit var collector: ModuleDependencyCollector

        override fun visitStringTemplateExpression(expression: KtStringTemplateExpression) {
            if (!expression.isInsideBuildGradleKtsFile()) return

            val module = expression.containingFile.module ?: return
            if (!this::collector.isInitialized) {
                collector = ModuleDependencyCollector()
                collector.collectDependencies(module)
            }

            processDependencyDeclaration(expression)

            super.visitStringTemplateExpression(expression)
        }

        private fun processDependencyDeclaration(expression: KtStringTemplateExpression) {
            val callParents = expression.parentsOfType<KtCallExpression>().toList()
            when {
                callParents.isLibraryDependencyDeclaration() ->
                    processLibraryDependencyDeclaration(expression, callParents[0])
                callParents.isModuleDependencyDeclaration() ->
                    processModuleDependencyDeclaration(expression, callParents[1])
            }
        }

        private fun processLibraryDependencyDeclaration(expression: KtStringTemplateExpression, elementToReport: PsiElement) {
            val libraryName = expression.text.withoutQuotes()
            val libraryEntries = collector.getLibraryEntries(libraryName)
            val finder = LibraryDependencyDeclarationFinder(libraryName)
            registerProblemIfNeeded(expression, elementToReport, libraryEntries, finder)
        }

        private fun processModuleDependencyDeclaration(expression: KtStringTemplateExpression, elementToReport: PsiElement) {
            val moduleEntries = collector.getModuleEntries(expression.getIntellijModuleName())
            val finder = ModuleDependencyDeclarationFinder(expression.text.withoutQuotes())
            registerProblemIfNeeded(expression, elementToReport, moduleEntries, finder)
        }

        private fun registerProblemIfNeeded(
            expression: KtStringTemplateExpression,
            elementToReport: PsiElement,
            orderEntries: List<OrderEntry>,
            finder: KtElementFinder
        ) {
            if (orderEntries.isNotEmpty()) {
                val modules = orderEntries.map { it.ownerModule }
                val navigationQuickFix = NavigateToOneOfPreviousDeclarationsQuickFix.createFrom(
                    expression, modules, finder
                )
                registerProblem(elementToReport, deleteDependencyQuickFix, navigationQuickFix)
            }
        }

        private fun registerProblem(element: PsiElement, vararg quickFixes: LocalQuickFix?) {
            holder.registerProblem(
                element,
                DESCRIPTION_TEMPLATE,
                ProblemHighlightType.WARNING,
                *quickFixes
            )
        }
    }

    class DeleteDependencyQuickFix : LocalQuickFix {
        override fun getName() = QUICK_FIX_NAME

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            descriptor.psiElement.delete()
        }
    }
}

private fun KtStringTemplateExpression.getIntellijModuleName() =
    "${project.getModuleNamePrefix()}${text.toIntellijModuleName()}"
