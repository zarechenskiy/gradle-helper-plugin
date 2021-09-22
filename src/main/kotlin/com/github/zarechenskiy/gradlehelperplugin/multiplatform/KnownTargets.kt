package com.github.zarechenskiy.gradlehelperplugin.multiplatform

import com.github.zarechenskiy.gradlehelperplugin.GradleHelperPluginBundle

internal object KnownTargets {
    val targetGroups = listOf(
        TargetGroup("mpp.target.jvm")
            .withTarget(TargetTemplate("jvm")),
        TargetGroup("mpp.target.js")
            .withTarget(
                TargetTemplate("js", requiresAtLeastOneChild = true)
                    .withChild(TargetTemplate("browser"))
                    .withChild(TargetTemplate("nodejs"))
            ),
        TargetGroup("mpp.target.linux")
            .withTarget(TargetTemplate("linuxX64"))
            .withTarget(TargetTemplate("linuxArm64")),
        TargetGroup("mpp.target.macos")
            .withTarget(TargetTemplate("macosX64"))
            .withTarget(TargetTemplate("macosArm64")),
        TargetGroup("mpp.target.windows")
            .withTarget(TargetTemplate("mingwX64"))
            .withTarget(TargetTemplate("mingwX86"))
        )

    val targetGradleNames = targetGroups.flatMap { group ->
        group.targets.flatMap {
            if (it.requiresAtLeastOneChild)
                it.children.map { child -> child.gradleName }
            else
                listOf(it.gradleName)
        }
    }.toSet()
}

class TargetGroup(val nameKey: String) {
    val targets: MutableList<TargetTemplate> = mutableListOf()

    fun getName() = GradleHelperPluginBundle.getMessage(nameKey)

    fun withTarget(target: TargetTemplate): TargetGroup {
        targets.add(target)
        return this
    }

    fun withTargetList(targetList: List<TargetTemplate>): TargetGroup {
        targets.clear()
        targets.addAll(targetList)
        return this
    }
}

class TargetTemplate(
    val gradleName: String,
    val requiresAtLeastOneChild: Boolean = false,
    var parent: TargetTemplate? = null
) {
    val children: MutableList<TargetTemplate> = mutableListOf()

    val stringTemplate: String = "$gradleName{\n}"

    fun withChild(template: TargetTemplate): TargetTemplate {
        template.parent = this
        children.add(template)
        return this
    }
}
