package com.github.zarechenskiy.gradlehelperplugin.inspections

import com.github.zarechenskiy.gradlehelperplugin.GradleHelperPluginBundle
import com.github.zarechenskiy.gradlehelperplugin.findScriptFile
import com.github.zarechenskiy.gradlehelperplugin.getParentModuleName
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.inspections.findExistingEditor
import org.jetbrains.kotlin.idea.util.projectStructure.allModules

class NavigateToOneOfPreviousDeclarationsQuickFix(private val editor: Editor, private val actionGroup: DefaultActionGroup) : LocalQuickFix {
    companion object {
        val POPUP_NAME = GradleHelperPluginBundle.message("inspection.navigate.to.previous.declaration.popup.name")
        val QUICK_FIX_NAME = GradleHelperPluginBundle.message("inspection.navigate.to.previous.declaration.quick.fix")

        fun createFrom(
            element: PsiElement,
            modules: List<Module>,
            finder: KtElementFinder
        ): NavigateToOneOfPreviousDeclarationsQuickFix? {
            val editor = element.findExistingEditor() ?: return null

            val moduleNamesAndDeclarations = getModuleNamesAndDeclarations(modules, finder)
            if (moduleNamesAndDeclarations.isEmpty()) {
                return null
            }

            val actionGroup = createActionGroup(moduleNamesAndDeclarations)
            return NavigateToOneOfPreviousDeclarationsQuickFix(editor, actionGroup)
        }
    }

    override fun getName() = QUICK_FIX_NAME

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        showPopup(editor, actionGroup)
    }

    private fun showPopup(editor: Editor, actionGroup: DefaultActionGroup) {
        if (actionGroup.childrenCount == 0) return

        val dataContext = DataManager.getInstance().getDataContext(editor.contentComponent)
        val popup = JBPopupFactory.getInstance().createActionGroupPopup(
            POPUP_NAME,
            actionGroup,
            dataContext,
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            false
        )
        popup.showInBestPositionFor(dataContext)
    }
}

private fun getModuleNamesAndDeclarations(modules: List<Module>, finder: KtElementFinder): List<Pair<String, Navigatable>> {
    val parentModules = modules.getParentModules()
    val moduleNamesAndDeclarations = mutableListOf<Pair<String, Navigatable>>()
    for (module in parentModules) {
        module.findScriptFile()?.accept(finder)
        val dependencyDeclaration = finder.foundElement as? Navigatable
        if (dependencyDeclaration != null) {
            moduleNamesAndDeclarations.add(Pair(module.name, dependencyDeclaration))
        }
        finder.clearSearchResult()
    }

    return moduleNamesAndDeclarations
}

private fun createActionGroup(moduleNamesAndDeclarations: List<Pair<String, Navigatable>>): DefaultActionGroup {
    val actionGroup = DefaultActionGroup()
    for ((name, declaration) in moduleNamesAndDeclarations) {
        actionGroup.add(object : DumbAwareAction(name) {
            override fun actionPerformed(e: AnActionEvent) {
                declaration.navigate(true)
            }
        })
    }

    return actionGroup
}

private fun List<Module>.getParentModules(): List<Module> {
    if (isEmpty()) return emptyList()

    val project = first().project
    val result = mutableListOf<Module>()
    val parentModuleNames = map { it.getParentModuleName() }.toSet()
    for (module in project.allModules()) {
        if (module.name in parentModuleNames) {
            result.add(module)
        }
    }

    return result
}
