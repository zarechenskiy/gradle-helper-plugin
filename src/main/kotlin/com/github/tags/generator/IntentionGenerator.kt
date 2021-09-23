package com.github.tags.generator

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.intentions.SelfTargetingIntention
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.buildExpression

abstract class SpecificOptionsGenerator(name: String, specificOption: String) :
  AbstractOptionsGenerator(
    name,
    "kotlinOptions { $specificOption }"
  )

abstract class AbstractOptionsGenerator(
  name: String,
  private val options: String
) : SelfTargetingIntention<KtElement>(KtElement::class.java, { name }) {
  override fun applyTo(element: KtElement, editor: Editor?) {
    val psiFactory = KtPsiFactory(element.project)

    val fileText = element.containingFile.text
    val useShortName =
      fileText.contains("import org.jetbrains.kotlin.gradle.tasks.KotlinCompile") ||
          fileText.contains("import org.jetbrains.kotlin.gradle.tasks.*")

    val task = if (useShortName) "KotlinCompile" else "org.jetbrains.kotlin.gradle.tasks.KotlinCompile"

    val block = psiFactory.buildExpression {
      appendNonFormattedText(
        """
        tasks.withType<$task>().configureEach {
          $options
        }
        """.trimIndent()
      )
    }

    block.add(psiFactory.createNewLine(2))

    val caretModel = editor?.caretModel ?: return
    val elementToInsert = element.containingFile.findElementAt(caretModel.offset) ?: return

    element.containingFile.addAfter(block, elementToInsert)
  }

  override fun isApplicableTo(element: KtElement, caretOffset: Int): Boolean {
    if (!element.isValid) return false
    if (!element.containingKtFile.isScript()) return false

    return true
  }

}
