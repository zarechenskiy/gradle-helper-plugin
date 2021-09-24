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
        TargetGroup("mpp.target.android")
            .withTarget(Target("android")),
        TargetGroup("mpp.target.android.ndk")
            .withTarget(Target("androidNativeArm32"))
            .withTarget(Target("androidNativeArm64")),
        TargetGroup("mpp.target.ios")
            .withTarget(Target("iosArm32"))
            .withTarget(Target("iosArm64"))
            .withTarget(Target("iosX64"))
            .withTarget(Target("iosSimulatorArm64")),
        TargetGroup("mpp.target.watchos")
            .withTarget(Target("watchosArm32"))
            .withTarget(Target("watchosArm64"))
            .withTarget(Target("watchosX86"))
            .withTarget(Target("watchosX64"))
            .withTarget(Target("watchosSimulatorArm64")),
        TargetGroup("mpp.target.tvos")
            .withTarget(Target("tvosArm64"))
            .withTarget(Target("tvosX64"))
            .withTarget(Target("tvosSimulatorArm64")),
        TargetGroup("mpp.target.macos")
            .withTarget(NativeTarget("macosX64"))
            .withTarget(NativeTarget("macosArm64")),
        TargetGroup("mpp.target.linux")
            .withTarget(NativeTarget("linuxArm64"))
            .withTarget(NativeTarget("linuxArm32Hfp"))
            .withTarget(NativeTarget("linuxMips32"))
            .withTarget(NativeTarget("linuxMipsel32"))
            .withTarget(NativeTarget("linuxX64")),
        TargetGroup("mpp.target.windows")
            .withTarget(NativeTarget("mingwX64"))
            .withTarget(NativeTarget("mingwX86")),
        TargetGroup("mpp.target.wasm")
            .withTarget(Target("wasm32")),
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

open class Target(
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

class NativeTarget(
    override val gradleName: String,
    requiresEnvironment: Boolean = false
) : Target(gradleName, requiresEnvironment) {
    override val stringTemplate: String
        get() = """
        ${gradleName}(){
            binaries {
                executable {
                    // Binary configuration.
                }
            }
        }
        """.trimIndent()
}

class Environment(
    override val gradleName: String,
    var parent: Target? = null
) : GradleKtsMultiplatformTemplate {
    val parentGradleName: String?
        get() = parent?.gradleName
}
