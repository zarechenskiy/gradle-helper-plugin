package com.github.zarechenskiy.gradlehelperplugin.documentation

import com.github.zarechenskiy.gradlehelperplugin.*
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup.CONTENT_END
import com.intellij.lang.documentation.DocumentationMarkup.CONTENT_START
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.idea.KotlinDocumentationProvider
import org.jetbrains.kotlin.nj2k.postProcessing.resolve
import org.jetbrains.kotlin.psi.KtReferenceExpression

class DependencyDeclarationDocumentationProvider : AbstractDocumentationProvider() {
    companion object {
        private const val GRADLE_DOCUMENTATION_LINK =
            "https://docs.gradle.org/current/userguide/java_library_plugin.html#sec:java_library_configurations_graph"
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
                    content { p { append(fetchedDoc) } }
                    content {
                        p {
                            append("See also: <a href=\"$GRADLE_DOCUMENTATION_LINK\">$GRADLE_DOCUMENTATION_LINK</a>")
                        }
                    }
                }
            }
        }
        return null
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

private fun StringBuilder.content(f: StringBuilder.() -> Unit) {
    append(CONTENT_START)
    f()
    append(CONTENT_END)
}

private fun StringBuilder.p(f: StringBuilder.() -> Unit) {
    append("<p>")
    f()
    append("</p>")
}

private fun PsiElement.isDependencyDeclaration() =
    isInsideBuildGradleKtsFile() &&
    isInsideDependenciesBlock() &&
    text in DEPENDENCY_DECLARATION_LEVELS

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
