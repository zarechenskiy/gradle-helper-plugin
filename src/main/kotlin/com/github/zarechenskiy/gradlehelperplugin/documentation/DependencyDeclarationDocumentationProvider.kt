package com.github.zarechenskiy.gradlehelperplugin.documentation

import com.github.zarechenskiy.gradlehelperplugin.*
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup.CONTENT_END
import com.intellij.lang.documentation.DocumentationMarkup.CONTENT_START
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.parentsOfType
import org.jetbrains.kotlin.idea.KotlinDocumentationProvider
import org.jetbrains.kotlin.nj2k.postProcessing.resolve
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import java.lang.StringBuilder

class DependencyDeclarationDocumentationProvider : AbstractDocumentationProvider() {
    companion object {
        private const val SEE_ALSO_APPENDIX =
            "See also: <a href=\"https://docs.gradle.org/current/userguide/java_library_plugin.html#sec:java_library_configurations_graph\">https://docs.gradle.org/current/userguide/java_library_plugin.html#sec:java_library_configurations_graph</a>"
    }

    private val kotlinDocumentationProvider = KotlinDocumentationProvider()

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        if (element is KtReferenceExpression && element.isDependencyDeclaration()) {
            val resolvedElement = element.resolve()
            return buildString {
                if (resolvedElement != null) {
                    append(kotlinDocumentationProvider.generateDoc(resolvedElement, element))
                }
                val fetchedDoc = getDocumentation(element.text)
                if (fetchedDoc != null) {
                    append(CONTENT_START)
                    p { append(fetchedDoc) }
                    p { append(SEE_ALSO_APPENDIX) }
                    append(CONTENT_END)
                }
            }
        }
        return null
    }

    private fun StringBuilder.p(f: StringBuilder.() -> Unit) {
        append("<p>")
        f()
        append("</p>")
    }

    override fun getCustomDocumentationElement(
        editor: Editor,
        file: PsiFile,
        contextElement: PsiElement?,
        targetOffset: Int
    ): PsiElement? {
        if (contextElement != null && contextElement.isDependencyDeclaration()) {
            return contextElement.parentOfType<KtReferenceExpression>()
        }
        return null
    }
}

private fun PsiElement.isDependencyDeclaration() =
    isInsideBuildGradleKtsFile() &&
    isInsideDependenciesBlock() &&
    text in DEPENDENCY_DECLARATION_LEVELS

private fun PsiElement.isInsideDependenciesBlock() =
    parentsOfType<KtCallExpression>().lastOrNull()?.calleeExpression?.text == DEPENDENCIES_BLOCK_NAME

private fun getDocumentation(functionName: String) =
    when (functionName) {
        DEPENDENCY_DECLARATION_LEVEL_API -> GradleHelperPluginBundle.message("documentation.dependency.declaration.level.api")
        DEPENDENCY_DECLARATION_LEVEL_IMPLEMENTATION -> GradleHelperPluginBundle.message("documentation.dependency.declaration.level.implementation")
        DEPENDENCY_DECLARATION_LEVEL_COMPILE_ONLY -> GradleHelperPluginBundle.message("documentation.dependency.declaration.level.compile.only")
        DEPENDENCY_DECLARATION_LEVEL_COMPILE_ONLY_API -> GradleHelperPluginBundle.message("documentation.dependency.declaration.level.compile.only.api")
        DEPENDENCY_DECLARATION_LEVEL_RUNTIME_ONLY -> GradleHelperPluginBundle.message("documentation.dependency.declaration.level.runtime.only")
        DEPENDENCY_DECLARATION_LEVEL_TEST_IMPLEMENTATION -> GradleHelperPluginBundle.message("documentation.dependency.declaration.level.test.implementation")
        DEPENDENCY_DECLARATION_LEVEL_TEST_COMPILE_ONLY -> GradleHelperPluginBundle.message("documentation.dependency.declaration.level.test.compile.only")
        DEPENDENCY_DECLARATION_LEVEL_TEST_RUNTIME_ONLY -> GradleHelperPluginBundle.message("documentation.dependency.declaration.level.test.runtime.only")
        else -> null
    }
