package com.github.zarechenskiy.gradlehelperplugin.multiplatform

import com.intellij.psi.*
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtVariableDeclaration
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer

data class Section<E : PsiElement>(
    val name: String,
    val psiRoot: SmartPsiElementPointer<E>,
    val label: String? = null
)

private fun calleeNameFilter(name: String): (PsiElement) -> Boolean = {
    it is KtCallExpression && it.calleeExpression?.text == name
}

private fun findSections(root: PsiElement, predicate: (KtCallExpression) -> Boolean): List<Section<KtCallExpression>> {
    return root
        .findAllChildrenByPredicate(predicate)
        .map { Section(it.calleeExpression?.text ?: "", it.createSmartPointer()) }
}

private fun mapSectionsToBlocks(sections: List<Section<KtCallExpression>>) : List<Section<KtLambdaArgument>> {
    val result = mutableListOf<Section<KtLambdaArgument>>()
    for (section in sections) {
        val root = section.psiRoot.element ?: continue
        val name = root.calleeExpression?.text ?: continue
        for (arg in root.valueArguments) {
            if (arg is KtLambdaArgument) {
                result.add(Section(name, arg.createSmartPointer()))
            }
        }
    }
    return result
}

private fun findConfigurations(file: PsiFile): List<Section<KtLambdaArgument>> {
    return mapSectionsToBlocks(findSections(file, calleeNameFilter("kotlin")))
}

private fun findTargetEntries(configurationSections: List<Section<KtLambdaArgument>>): List<Section<KtCallExpression>> {
    val result = mutableListOf<Section<KtCallExpression>>()
    for (cfg in configurationSections) {
        val root = cfg.psiRoot.element ?: continue
        result.addAll(findSections(root) { it.calleeExpression?.text in KnownTargets.targetGradleNames })
    }
    return result
}

private fun findTargetEntries(file: PsiFile): List<Section<KtCallExpression>> {
    val configurationSections = findConfigurations(file)
    return findTargetEntries(configurationSections)
}

private fun findEnvironmentEntries(
    section: Section<KtLambdaArgument>,
    parentGradleName: String
): List<Section<KtCallExpression>> {
    val environmentNames = KnownTargets.environmentGradleNames[parentGradleName] ?: return emptyList()
    val root = section.psiRoot.element ?: return emptyList()
    return findSections(root) { it.calleeExpression?.text in environmentNames }
}

fun findTargetGradleNames(file: PsiFile): Map<String, Set<String>> {
    val existingNames = mutableMapOf<String, MutableSet<String>>()
    val existingTargetEntries = mapSectionsToBlocks(findTargetEntries(file))
    for (entry in existingTargetEntries) {
        val gradleName = entry.name
        if (gradleName !in existingNames) existingNames[gradleName] = mutableSetOf()
        if (gradleName in KnownTargets.environmentGradleNames) {
            val existingEnvironmentEntries = findEnvironmentEntries(entry, gradleName)
            for (envEntry in existingEnvironmentEntries) {
                existingNames[gradleName]?.add(envEntry.name)
            }
        }
    }
    return existingNames
}

private fun findMainConfiguration(file: PsiFile): Section<KtLambdaArgument>? {
    val configurationSections = findConfigurations(file)
    if (configurationSections.isEmpty()) return null
    return configurationSections[0]
}

fun findSourceSets(file: PsiFile, configurationSection: Section<KtLambdaArgument>? = null): List<Section<KtLambdaArgument>> {
    return mapSectionsToBlocks(findSections(file, calleeNameFilter("sourceSets")))
}

fun findOrCreateSourceSets(file: PsiFile, configurationSection: Section<KtLambdaArgument>? = null): List<Section<KtLambdaArgument>> {
    var sourceSetsBlocks = mapSectionsToBlocks(findSections(file, calleeNameFilter("sourceSets")))
    if (sourceSetsBlocks.isEmpty()) {
        val configurationExpression = configurationSection ?: findMainConfiguration(file) ?: return emptyList()
        val configuration = configurationExpression.psiRoot.element?.getLambdaExpression() ?: return emptyList()
        val psiFactory = KtPsiFactory(file)
        val targetExpression = psiFactory.createExpression("sourceSets {\n}")
        val configurationBody = configuration.bodyExpression ?: return emptyList()
        val addedTargetBlock = configurationBody.addBefore(targetExpression, configurationBody.firstStatement)
        configurationBody.addAfter(psiFactory.createWhiteSpace("\n\n"), addedTargetBlock)
        sourceSetsBlocks = mapSectionsToBlocks(findSections(file, calleeNameFilter("sourceSets")))
    }
    return sourceSetsBlocks
}

fun addMultiplatformTarget(
    file: SmartPsiElementPointer<PsiFile>,
    target: Target,
    configurationSection: Section<KtLambdaArgument>? = null
) {
    val psiFile = file.element ?: return
    val configurationExpression = configurationSection ?: findMainConfiguration(psiFile) ?: return
    val configuration = configurationExpression.psiRoot.element?.getLambdaExpression() ?: return

    val psiFactory = KtPsiFactory(psiFile)
    val targetExpression = psiFactory.createExpression(target.stringTemplate)
    val configurationBody = configuration.bodyExpression ?: return
    val addedTargetBlock = configurationBody.addBefore(targetExpression, configurationBody.firstStatement)
    configurationBody.addAfter(psiFactory.createWhiteSpace("\n\n"), addedTargetBlock)

    val sourceSets = findOrCreateSourceSets(psiFile, configurationExpression)
    for (sourceSet in sourceSets) {
        val sourceSetRoot = sourceSet.psiRoot.element ?: continue
        val sourceSetBody = sourceSetRoot.getLambdaExpression()?.bodyExpression ?: continue
        val targetSets = listOf(
            psiFactory.createDeclaration<KtVariableDeclaration>("val ${target.gradleName}Test by getting"),
            psiFactory.createDeclaration<KtVariableDeclaration>("val ${target.gradleName}Main by getting"),
        )

        for (newSet in targetSets) {
            val addedSet = sourceSetBody.addBefore(newSet, sourceSetBody.firstStatement)
            sourceSetBody.addAfter(psiFactory.createWhiteSpace("\n"), addedSet)
        }
    }
}

fun addMultiplatformEnvironment(
    file: SmartPsiElementPointer<PsiFile>,
    environment: Environment,
    configurationSection: Section<KtLambdaArgument>? = null
) {
    val parent = environment.parent ?: return
    val parentGradleName = environment.parentGradleName ?: return
    val psiFile = file.element ?: return
    val configurationExpression = configurationSection ?: findMainConfiguration(psiFile) ?: return
    val configurationPsiRoot = configurationExpression.psiRoot.element ?: return
    var targetBlocks = mapSectionsToBlocks(findSections(configurationPsiRoot, calleeNameFilter(parentGradleName)))
    if (targetBlocks.isEmpty()) {
        addMultiplatformTarget(file, parent)
        targetBlocks = mapSectionsToBlocks(findSections(configurationPsiRoot, calleeNameFilter(parentGradleName)))
    }

    if (targetBlocks.isEmpty()) return
    val psiFactory = KtPsiFactory(psiFile)
    for (block in targetBlocks) {
        val blockRoot = block.psiRoot.element?.getLambdaExpression() ?: continue
        val environmentExpression = psiFactory.createExpression(environment.stringTemplate)
        val body = blockRoot.bodyExpression ?: return
        val addedEnvironmentBlock = body.addAfter(environmentExpression, body.firstStatement)
        body.addAfter(psiFactory.createWhiteSpace("\n"), addedEnvironmentBlock)
    }
}

inline fun <reified E : PsiElement> PsiElement.findAllChildrenByPredicate(
    crossinline predicate: (E) -> Boolean
): List<E> {
    val matchingElements = mutableListOf<E>()
    accept(object : PsiRecursiveElementVisitor() {
        override fun visitElement(element: PsiElement) {
            if (element is E && predicate(element)) matchingElements.add(element)
            super.visitElement(element)
        }
    })
    return matchingElements
}
