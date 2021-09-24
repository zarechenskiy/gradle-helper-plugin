package com.github.zarechenskiy.gradlehelperplugin.multiplatform

import com.intellij.psi.*
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

private fun findSectionsByName(parent: PsiElement, name: String): List<KtLambdaArgument> {
    return parent
        .findAllChildrenByPredicate { it is KtCallExpression && it.calleeExpression?.text == name }
        .mapNotNull { it.safeAs<KtCallExpression>() }
        .flatMap { it.valueArguments.mapNotNull { arg -> arg.safeAs<KtLambdaArgument>() } }
        .toList()
}

private fun findConfigurationBlocks(file: PsiFile): List<KtLambdaArgument> {
    return findSectionsByName(file, "kotlin")
}

private data class TargetEntry(
    val gradleName: String,
    val targetLabel: String?,
    val psiRoot: SmartPsiElementPointer<KtCallExpression>
)

private fun findExistingTargetEntries(configurationSections: List<KtLambdaArgument>): List<KtCallExpression> {
    return configurationSections.flatMap { block ->
        block
            .findAllChildrenByPredicate {
                it is KtCallExpression && it.calleeExpression?.text in KnownTargets.targetGradleNames
            }
            .mapNotNull { elt -> elt.safeAs<KtCallExpression>() }
    }.toList()
}

private fun findEnvironmentConfigurationEntries(root: PsiElement, parentGradleName: String): List<KtCallExpression> {
    val environmentNames = KnownTargets.environmentGradleNames[parentGradleName] ?: return emptyList()
    return root.findAllChildrenByPredicate { it is KtCallExpression && it.calleeExpression?.text in environmentNames }
        .mapNotNull { it.safeAs<KtCallExpression>() }
        .toList()
}

private fun findExistingTargetEntries(file: PsiFile): List<KtCallExpression> {
    val kotlinBlocks = findConfigurationBlocks(file)
    return findExistingTargetEntries(kotlinBlocks)
}

fun findExistingTargetGradleNames(file: PsiFile): Map<String, Set<String>> {
    val existingNames = mutableMapOf<String, MutableSet<String>>()
    val existingTargetEntries = findExistingTargetEntries(file)
    for (entry in existingTargetEntries) {
        val gradleName = entry.calleeExpression?.text ?: continue
        if (gradleName !in existingNames) existingNames[gradleName] = mutableSetOf()
        if (gradleName in KnownTargets.environmentGradleNames) {
            val existingEnvironmentEntries = findEnvironmentConfigurationEntries(entry, gradleName)
            for (envEntry in existingEnvironmentEntries) {
                val envGradleName = envEntry.calleeExpression?.text ?: continue
                existingNames[gradleName]?.add(envGradleName)
            }
        }
    }
    return existingNames
}

private fun findConfigurationSection(file: PsiFile): KtLambdaArgument? {
    val configurationBlocks = findConfigurationBlocks(file)
    if (configurationBlocks.isEmpty()) return null
    return configurationBlocks[0]
}

fun addMultiplatformTarget(
    file: SmartPsiElementPointer<PsiFile>,
    target: Target,
    configurationSection: KtLambdaArgument? = null
) {
    val psiFile = file.element ?: return
    val configurationExpression = configurationSection ?: findConfigurationSection(psiFile) ?: return
    val configuration = configurationExpression.getLambdaExpression() ?: return

    val psiFactory = KtPsiFactory(psiFile)
    val targetExpression = psiFactory.createExpression(target.stringTemplate)
    val configurationBody = configuration.bodyExpression ?: return
    val addedTargetBlock = configurationBody.addBefore(targetExpression, configurationBody.firstStatement)
    configurationBody.addAfter(psiFactory.createWhiteSpace("\n\n"), addedTargetBlock)
}

fun addMultiplatformEnvironment(file: SmartPsiElementPointer<PsiFile>, environment: Environment) {
}

fun PsiElement.findAllChildrenByPredicate(predicate: (PsiElement) -> Boolean): List<PsiElement> {
    val matchingElements = mutableListOf<PsiElement>()
    accept(object : PsiRecursiveElementVisitor() {
        override fun visitElement(element: PsiElement) {
            if (predicate(element)) matchingElements.add(element)
            super.visitElement(element)
        }
    })
    return matchingElements
}
