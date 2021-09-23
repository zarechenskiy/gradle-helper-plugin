package com.github.zarechenskiy.gradlehelperplugin.services

import com.github.zarechenskiy.gradlehelperplugin.resolve.GradleKtsModuleReferenceProvider
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar

class GradleKtsReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) =
        with(registrar) {
            registerReferenceProvider(
                PlatformPatterns.psiElement(PsiElement::class.java),
                GradleKtsModuleReferenceProvider()
            )
        }
}
