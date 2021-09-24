package com.github.zarechenskiy.gradlehelperplugin.multiplatform

import com.github.zarechenskiy.gradlehelperplugin.GradleHelperPluginBundle

internal object KnownTargets {
    val targetGroups = listOf(
        TargetGroup("mpp.target.jvm")
            .withTarget(Target("jvm")),

        TargetGroup("mpp.target.js")
            .withTarget(
                Target("js", requiresEnvironment = true)
                    .withEnvironment(Environment("browser"))
                    .withEnvironment(Environment("nodejs"))
            ),
        TargetGroup("mpp.target.linux")
            .withTarget(Target("linuxX64"))
            .withTarget(Target("linuxArm64")),

        TargetGroup("mpp.target.macos")
            .withTarget(Target("macosX64"))
            .withTarget(Target("macosArm64")),

        TargetGroup("mpp.target.windows")
            .withTarget(Target("mingwX64"))
            .withTarget(Target("mingwX86"))
        )

    val targetGradleNames = targetGroups.flatMap { it.targets.map { target -> target.gradleName } }.toSet()

    val environmentGradleNames: Map<String, Set<String>> = fillEnvironmentNames()

    private fun fillEnvironmentNames(): Map<String, Set<String>> {
        val nameMap = mutableMapOf<String, MutableSet<String>>()
        for (group in targetGroups) {
            for (target in group.targets) {
                nameMap[target.gradleName] = mutableSetOf()
                for (env in target.environments) {
                    nameMap[target.gradleName]?.add(env.gradleName)
                }
            }
        }
        return nameMap
    }
}

class TargetGroup(val nameKey: String) {
    val targets: MutableList<Target> = mutableListOf()

    fun getName() = GradleHelperPluginBundle.getMessage(nameKey)

    fun withTarget(target: Target): TargetGroup {
        targets.add(target)
        return this
    }
}

interface GradleKtsMultiplatformTemplate {
    val gradleName: String
    val stringTemplate: String
        get() = "${gradleName}(){\n}"
}

class Target(
    override val gradleName: String,
    val requiresEnvironment: Boolean = false
) : GradleKtsMultiplatformTemplate {
    val environments: MutableList<Environment> = mutableListOf()

    fun withEnvironment(environment: Environment): Target {
        environment.parent = this
        environments.add(environment)
        return this
    }
}

class Environment(
    override val gradleName: String,
    var parent: Target? = null
) : GradleKtsMultiplatformTemplate {
    val parentGradleName: String?
        get() = parent?.gradleName
}
