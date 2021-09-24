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
    val fqNameString = checkIsBinaryExpression(file, offset)
    return fqNameString == "org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions.freeCompilerArgs" ||
        fqNameString == "org.jetbrains.kotlin.gradle.dsl.KotlinCommonToolOptions.freeCompilerArgs"
  }
}

class DefaultJvmArgsContext : TemplateContextType("gradle.helper.applicationDefaultJvmArgs", "Java compiler arguments context") {
  override fun isInContext(file: PsiFile, offset: Int): Boolean {
    val fqNameString = checkIsBinaryExpression(file, offset)
    return fqNameString == "applicationDefaultJvmArgs"
  }
}

private fun checkIsBinaryExpression(file: PsiFile, offset: Int): String? {
  val elementAt = checkIsScript(file, offset) ?: return null
  val string = elementAt.parent.parent
  if (string !is KtStringTemplateExpression) return null
  val binaryExpr = string.parent.parent.parent.parent
  if (binaryExpr !is KtBinaryExpression) return null

  val freeCompilerArgs = binaryExpr.left ?: return null
  val resultingDescriptor = freeCompilerArgs.resolveToCall()?.resultingDescriptor ?: return null
  return resultingDescriptor.fqNameOrNull()?.asString()
}

private fun checkIsScript(file: PsiFile, offset: Int): PsiElement? {
  if (file !is KtFile) return null
  if (!file.isScript()) return null

  return file.findElementAt(offset)
}
