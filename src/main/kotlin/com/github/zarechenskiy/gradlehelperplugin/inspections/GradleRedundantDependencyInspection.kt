package com.github.zarechenskiy.gradlehelperplugin.inspections

import com.github.zarechenskiy.gradlehelperplugin.*
import com.github.zarechenskiy.gradlehelperplugin.GRADLE_LIBRARY_NAME_PREFIX
import com.github.zarechenskiy.gradlehelperplugin.INTELLIJ_MODULE_NAME_DELIMITER
import com.github.zarechenskiy.gradlehelperplugin.MAIN_SUBMODULE_NAME
import com.intellij.codeInspection.*
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.parentsOfType
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import org.jetbrains.kotlin.idea.util.projectStructure.module
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid

class GradleKtsRedundantDependencyInspection : LocalInspectionTool(), CleanupLocalInspectionTool {
    companion object {
        private val DESCRIPTION_TEMPLATE = GradleHelperPluginBundle.message("inspection.redundant.dependency.problem.descriptor")
        private val QUICK_FIX_NAME = GradleHelperPluginBundle.message("inspection.redundant.dependency.quickfix")
    }

    private val quickFix = GradleKtsRedundantDependencyQuickFix()

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        GradleKtsRedundantDependencyVisitor(holder)

    inner class GradleKtsRedundantDependencyVisitor(private val holder: ProblemsHolder) : KtVisitorVoid() {
        private var implementationLibraries: Set<String>? = null
        private var moduleDependencies: Set<String>? = null

        override fun visitStringTemplateExpression(expression: KtStringTemplateExpression) {
            if (!expression.isInsideBuildGradleKtsFile()) return

            initDependenciesSets(expression.containingFile)
            processDependencyDeclaration(expression)

            super.visitStringTemplateExpression(expression)
        }

        private fun initDependenciesSets(file: PsiFile) {
            // TODO: handle test dependencies correctly
            if (implementationLibraries == null) {
                val moduleName = file.module?.name ?: return
                val mainModule = file.project.getMainSubmodule(moduleName) ?: return
                implementationLibraries = mainModule.getLibraryNamesFromParentModules()
                moduleDependencies = mainModule.getDependencyModuleNamesFromParentModules()
            }
        }

        private fun processDependencyDeclaration(expression: KtStringTemplateExpression) {
            val callParents = expression.parentsOfType<KtCallExpression>().toList()
            when {
                callParents.isLibraryDependencyDeclaration() -> {
                    val libraries = implementationLibraries ?: return
                    if (expression.text.withoutQuotes() in libraries) {
                        registerProblem(callParents[0])
                    }
                }
                callParents.isModuleDependencyDeclaration() -> {
                    val modules = moduleDependencies ?: return
                    if (expression.getIntellijModuleName() in modules) {
                        registerProblem(callParents[1])
                    }
                }
            }
        }

        private fun registerProblem(element: PsiElement) {
            holder.registerProblem(
                element,
                DESCRIPTION_TEMPLATE,
                ProblemHighlightType.WARNING,
                quickFix
            )
        }
    }

    class GradleKtsRedundantDependencyQuickFix : LocalQuickFix {
        override fun getName() = QUICK_FIX_NAME

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            descriptor.psiElement.delete()
        }
    }
}

private fun Project.getMainSubmodule(moduleName: String): Module? =
    allModules().firstOrNull {
        it.name == "$moduleName$INTELLIJ_MODULE_NAME_DELIMITER$MAIN_SUBMODULE_NAME"
    }

private fun Module.getDependencies(): Array<Module> =
    ModuleRootManager.getInstance(this).dependencies

private fun Module.getLibraries(): List<LibraryOrderEntry> =
    ModuleRootManager.getInstance(this).orderEntries.filterIsInstance<LibraryOrderEntry>()

private fun Module.getLibraryNamesFromParentModules(): Set<String> {
    val libraryNames = mutableSetOf<String>()
    for (module in getDependencies()) {
        libraryNames.addAll(
            module.getLibraries().mapNotNull {
                it.libraryName?.substringAfter(GRADLE_LIBRARY_NAME_PREFIX)
            }
        )
    }
    return libraryNames
}

private fun Module.getDependencyModuleNamesFromParentModules(): Set<String> {
    val moduleNames = mutableSetOf<String>()
    for (module in getDependencies()) {
        moduleNames.addAll(
            module.getDependencies().map {
                it.name.substringBefore(".$MAIN_SUBMODULE_NAME")
            }
        )
    }
    return moduleNames
}

private fun KtStringTemplateExpression.getIntellijModuleName() =
    "${project.getModuleNamePrefix()}${text.toIntellijModuleName()}"
