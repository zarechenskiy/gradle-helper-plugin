package com.github.zarechenskiy.gradlehelperplugin.actions

import com.github.zarechenskiy.gradlehelperplugin.BUILD_GRADLE_KTS_FILE_NAME
import com.github.zarechenskiy.gradlehelperplugin.multiplatform.*
import com.github.zarechenskiy.gradlehelperplugin.multiplatform.KnownTargets
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiFile


class AddMultiplatformTargetAction : AnAction() {
    private val LOG = Logger.getInstance(AddMultiplatformTargetAction::class.java)

    override fun update(e: AnActionEvent) {
        val fileIsCompatible = e.getData(CommonDataKeys.VIRTUAL_FILE)?.name == BUILD_GRADLE_KTS_FILE_NAME
        e.presentation.isEnabledAndVisible =  e.project != null && fileIsCompatible
    }

    override fun actionPerformed(e: AnActionEvent) {
        if (e.project == null) return
        val file = e.getData(CommonDataKeys.PSI_FILE) ?: return
        val existingTargets = findExistingTargetGradleNames(file)
        val actionGroup = createActionGroup(file, existingTargets)
        showPopup(e, actionGroup)
    }

    private fun createActionGroup(
        file: PsiFile,
        existingGradleNames: List<String>
    ): DefaultActionGroup {
        val actionGroup = DefaultActionGroup()
        val candidates = candidateTargetGroups(existingGradleNames)
        for (group in candidates) {
            val targetActionGroup = DefaultActionGroup()
            for (target in group.targets) {
                targetActionGroup.add(object : DumbAwareAction(target.gradleName) {
                    override fun actionPerformed(e: AnActionEvent) {
                        WriteCommandAction.runWriteCommandAction(e.project) {
                            addMultiplatformTarget(file, target)
                        }
                    }
                })
            }

            actionGroup.add(object : DumbAwareAction(group.getName()) {
                override fun actionPerformed(e: AnActionEvent) {
                    showPopup(e, targetActionGroup)
                }
            })
        }

        return actionGroup
    }

    private fun candidateTargetGroups(existingGradleNames: List<String>): List<TargetGroup> {
        fun candidateTargets(targets: List<TargetTemplate>): List<TargetTemplate> {
            val newTargets = mutableListOf<TargetTemplate>()
            for (target in targets) {
                if (target.requiresAtLeastOneChild) {
                    newTargets.addAll(candidateTargets(target.children))
                } else if (target.gradleName !in existingGradleNames) {
                    newTargets.add(target)
                }
            }
            return newTargets
        }

        val newTargetGroups = mutableListOf<TargetGroup>()
        for (group in KnownTargets.targetGroups) {
            val newTargets = candidateTargets(group.targets)
            if (newTargets.isNotEmpty()) {
                newTargetGroups.add(TargetGroup(group.nameKey).withTargetList(newTargets))
            }
        }

        return newTargetGroups
    }

    private fun showPopup(e: AnActionEvent, actionGroup: DefaultActionGroup) {
        if (actionGroup.childrenCount == 0) return
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
