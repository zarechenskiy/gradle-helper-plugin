package com.github.zarechenskiy.gradlehelperplugin.actions

import com.github.zarechenskiy.gradlehelperplugin.BUILD_GRADLE_KTS_FILE_NAME
import com.github.zarechenskiy.gradlehelperplugin.multiplatform.*
import com.github.zarechenskiy.gradlehelperplugin.multiplatform.KnownTargets
import com.github.zarechenskiy.gradlehelperplugin.multiplatform.Target
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer


class AddMultiplatformTargetAction : AnAction() {
    private val LOG = Logger.getInstance(AddMultiplatformTargetAction::class.java)

    override fun update(e: AnActionEvent) {
        val fileIsCompatible = e.getData(CommonDataKeys.VIRTUAL_FILE)?.name == BUILD_GRADLE_KTS_FILE_NAME
        e.presentation.isEnabledAndVisible =  e.project != null && fileIsCompatible
    }

    override fun actionPerformed(e: AnActionEvent) {
        if (e.project == null) return
        val file = e.getData(CommonDataKeys.PSI_FILE) ?: return
        val excludedNames = findExistingTargetGradleNames(file)
        val actionGroup = createActionGroup(file.createSmartPointer(), excludedNames)
        showPopup(e, actionGroup)
    }

    private fun <T> createConfigurationAction(
        name: String,
        file: SmartPsiElementPointer<PsiFile>,
        entry: T,
        updateAction: (SmartPsiElementPointer<PsiFile>, T) -> Unit
    ): AnAction {
        return object : DumbAwareAction(name) {
            override fun actionPerformed(e: AnActionEvent) {
                WriteCommandAction.runWriteCommandAction(e.project) {
                    updateAction(file, entry)
                }
            }
        }
    }

    private fun createSubmenuAction(
        name: String,
        actionGroup: DefaultActionGroup
    ): AnAction {
        return object : DumbAwareAction(name) {
            override fun actionPerformed(e: AnActionEvent) {
                showPopup(e, actionGroup)
            }

        }
    }

    private fun createEnvironmentActionGroup(
        file: SmartPsiElementPointer<PsiFile>,
        parentTarget: Target,
        excludedNames: Map<String, Set<String>>
    ): DefaultActionGroup {
        val actionGroup = DefaultActionGroup()
        for (env in parentTarget.environments) {
            val parentName = env.parentGradleName ?: continue
            if (excludedNames[parentName]?.contains(env.gradleName) != true) {
                actionGroup.add(createConfigurationAction(env.gradleName, file, env, ::addMultiplatformEnvironment))
            }
        }
        return actionGroup
    }

    private fun createTargetActionGroup(
        file: SmartPsiElementPointer<PsiFile>,
        targetGroup: TargetGroup,
        excludedNames: Map<String, Set<String>>
    ): DefaultActionGroup {
        val actionGroup = DefaultActionGroup()
        for (target in targetGroup.targets) {
            if (target.gradleName !in excludedNames) {
                if (target.requiresEnvironment) {
                    // As the target requires an environment, we should create a submenu with environments
                    // And exclude the target is there are no environments
                    val environmentMenu = createEnvironmentActionGroup(file, target, excludedNames)
                    if (environmentMenu.isNotEmpty()) {
                        actionGroup.add(createSubmenuAction(target.gradleName, environmentMenu))
                    }
                } else {
                    val createTargetAction = createConfigurationAction(
                        target.gradleName,
                        file,
                        target,
                        ::addMultiplatformTarget
                    )

                    if (target.environments.isEmpty()) {
                        // If there are no environments, just create a single "create target" action
                        actionGroup.add(createTargetAction)
                    } else {
                        // If there are environments, create a submenu with the target itself and its environments
                        val environmentMenu = DefaultActionGroup()
                        environmentMenu.add(createTargetAction)
                        environmentMenu.addAll(createEnvironmentActionGroup(file, target, excludedNames))
                        actionGroup.add(createSubmenuAction(target.gradleName, environmentMenu))
                    }
                }
            }
        }
        return actionGroup
    }

    private fun createActionGroup(
        file: SmartPsiElementPointer<PsiFile>,
        excludedNames: Map<String, Set<String>>
    ): DefaultActionGroup {
        val actionGroup = DefaultActionGroup()
        for (group in KnownTargets.targetGroups) {
            val targetActionGroup = createTargetActionGroup(file, group, excludedNames)
            if (targetActionGroup.isNotEmpty()) {
                actionGroup.add(createSubmenuAction(group.getName(), targetActionGroup))
            }
        }
        return actionGroup
    }

    private fun showPopup(e: AnActionEvent, actionGroup: DefaultActionGroup) {
        if (actionGroup.isEmpty()) return
        val popup = JBPopupFactory.getInstance().createActionGroupPopup(
            e.presentation.text,
            actionGroup,
            e.dataContext,
            JBPopupFactory.ActionSelectionAid.NUMBERING,
            false
        )
        popup.showInBestPositionFor(e.dataContext)
    }
}

private fun DefaultActionGroup.isEmpty(): Boolean = childrenCount == 0
private fun DefaultActionGroup.isNotEmpty(): Boolean = !isEmpty()
