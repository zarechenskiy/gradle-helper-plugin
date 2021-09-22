package com.github.tags.generator

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.intentions.SelfTargetingIntention
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory

class IntentionGenerator : SelfTargetingIntention<KtFile>(KtFile::class.java, { "something" }) {

  override fun applyTo(element: KtFile, editor: Editor?) {
    val cls = KtPsiFactory(element.project).createClass("class Foo")
    element.psiOrParent.replace(cls)
  }

  override fun isApplicableTo(element: KtFile, caretOffset: Int): Boolean {
    if (!element.isScript()) return false

    return true
  }
}