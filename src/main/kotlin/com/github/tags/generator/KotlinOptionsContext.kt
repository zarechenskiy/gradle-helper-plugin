package com.github.tags.generator

import com.intellij.codeInsight.template.TemplateContextType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScriptInitializer
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.isTopLevelInFileOrScript
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull

class KotlinOptionsContext : TemplateContextType("gradle.helper.toplevel", "Scripts top-level") {
  override fun isInContext(file: PsiFile, offset: Int): Boolean {
    val elementAt = checkIsScript(file, offset) ?: return false
    return isTopLevelInFileOrScript(elementAt) || elementAt.parent.parent is KtScriptInitializer
  }
}

class FreeCompilerArgsContext : TemplateContextType("gradle.helper.freeCompilerArgs", "Compiler arguments context") {
  override fun isInContext(file: PsiFile, offset: Int): Boolean {
    val elementAt = checkIsScript(file, offset) ?: return false
    val string = elementAt.parent.parent
    if (string !is KtStringTemplateExpression) return false
    val binaryExpr = string.parent
    if (binaryExpr !is KtBinaryExpression) return false

    val freeCompilerArgs = binaryExpr.left ?: return false
    val fqName = freeCompilerArgs.resolveToCall()?.resultingDescriptor?.fqNameOrNull() ?: return false
    return fqName.asString() == "org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions.freeCompilerArgs" ||
        fqName.asString() == "org.jetbrains.kotlin.gradle.dsl.KotlinCommonToolOptions.freeCompilerArgs"
  }
}

private fun checkIsScript(file: PsiFile, offset: Int): PsiElement? {
  if (file !is KtFile) return null
  if (!file.isScript()) return null

  return file.findElementAt(offset)
}
