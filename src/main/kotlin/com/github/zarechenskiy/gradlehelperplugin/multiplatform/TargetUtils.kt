package com.github.zarechenskiy.gradlehelperplugin.multiplatform

import com.intellij.psi.*
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

private fun findConfigurationBlocks(file: PsiFile): List<KtLambdaArgument> {
    return file
        .getChildrenByPredicate { it is KtCallExpression && it.calleeExpression?.text == "kotlin" }
        .asSequence()
        .mapNotNull { it.safeAs<KtCallExpression>() }
        .filter { it.valueArguments.size == 1 }
        .mapNotNull { it.valueArguments[0].safeAs<KtLambdaArgument>() }
        .toList()
}

private fun findExistingTargetEntries(kotlinBlocks: List<KtLambdaArgument>): List<KtCallExpression> {
    return kotlinBlocks.flatMap { block ->
        block
            .getChildrenByPredicate {
                it is KtCallExpression && it.calleeExpression?.text in KnownTargets.targetGradleNames
            }
            .mapNotNull { elt -> elt.safeAs<KtCallExpression>() }
    }.toList()
}

private fun findExistingTargetEntries(file: PsiFile): List<KtCallExpression> {
    val kotlinBlocks = findConfigurationBlocks(file)
    return findExistingTargetEntries(kotlinBlocks)
}

fun findExistingTargetGradleNames(file: PsiFile): List<String> {
    return findExistingTargetEntries(file).mapNotNull { it.calleeExpression?.text }
}

fun addMultiplatformTarget(file: PsiFile, target: TargetTemplate) {
    val configurationBlocks = findConfigurationBlocks(file)
    if (configurationBlocks.isEmpty()) return
    val configuration = configurationBlocks[0].getLambdaExpression() ?: return
    val psiFactory = KtPsiFactory(file)
    if (target.parent == null) {
        val targetExpression = psiFactory.createExpression(target.stringTemplate)
        val configurationBody = configuration.bodyExpression ?: return
        val addedTargetBlock = configurationBody.addBefore(targetExpression, configurationBody.firstStatement)
        configurationBody.addAfter(psiFactory.createWhiteSpace("\n\n"), addedTargetBlock)
    }
}

fun PsiElement.getChildrenByPredicate(predicate: (PsiElement) -> Boolean): List<PsiElement> {
    val matchingElements = mutableListOf<PsiElement>()
    accept(object : PsiRecursiveElementVisitor() {
        override fun visitElement(element: PsiElement) {
            if (predicate(element)) matchingElements.add(element)
            super.visitElement(element)
        }
    })
    return matchingElements
}
