@file:Suppress("unused")
package ru.otus.homework.lintchecks.detector

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.intellij.psi.PsiClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.getContainingUClass
import ru.otus.homework.lintchecks.detector.GlobalScopeIssue.BRIEF_DESCRIPTION
import ru.otus.homework.lintchecks.detector.GlobalScopeIssue.GLOBAL_SCOPE
import ru.otus.homework.lintchecks.detector.GlobalScopeIssue.ISSUE


@Suppress("UnstableApiUsage")
class GlobalScopeDetector : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> {
        return listOf<Class<out UElement>>(USimpleNameReferenceExpression::class.java)
    }

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {

            override fun visitSimpleNameReferenceExpression(node: USimpleNameReferenceExpression) {
                if (node.identifier == GLOBAL_SCOPE) {
                    context.report(
                        issue = ISSUE,
                        scope = node,
                        location = context.getLocation(node),
                        message = BRIEF_DESCRIPTION,
                        quickfixData = createFix(context, node)
                    )
                }
            }
        }
    }

    private fun createFix(
        context: JavaContext,
        node: USimpleNameReferenceExpression
    ): LintFix? {
        val psiClass = node.getContainingUClass()?.javaPsi

        if (psiClass?.hasParent(context, "androidx.lifecycle.ViewModel") == true) {
            return createViewModelScopeFix(
                context, node
            )
        }

        if (psiClass?.hasParent(context, "androidx.fragment.app.Fragment") == true) {
            return createFragmentScopeFix(context, node)
        }

        return null
    }

    private fun createViewModelScopeFix(
        context: JavaContext,
        node: USimpleNameReferenceExpression
    ): LintFix? {
        val hasViewModelArtifact =
            hasArtifact(context, "androidx.lifecycle:lifecycle-viewmodel-ktx")

        if (!hasViewModelArtifact) return null

        return fix()
            .replace()
            .range(context.getLocation(node))
            .with("viewModelScope")
            .shortenNames()
            .reformat(true)
            .build()
    }

    private fun createFragmentScopeFix(
        context: JavaContext,
        node: USimpleNameReferenceExpression
    ): LintFix? {
        val hasFragmentArtifact = hasArtifact(context, "androidx.lifecycle:lifecycle-runtime-ktx")

        if (!hasFragmentArtifact) return null

        return fix()
            .replace()
            .range(context.getLocation(node))
            .with("viewLifecycleOwner.lifecycleScope")
            .shortenNames()
            .reformat(true)
            .build()
    }

    fun hasArtifact(context: JavaContext, artifactName: String): Boolean {
        return context.evaluator.dependencies?.getAll()?.any {
            it.identifier.contains(artifactName)
        } == true
    }
}

fun PsiClass.hasParent(context: JavaContext, parentName: String): Boolean {
    val className = parentName.substringAfterLast(".")
    val classPackage = parentName.substringBeforeLast(".")
    var psiClass = this
    while (psiClass.name != className) {
        psiClass = psiClass.superClass ?: return false
    }
    return context.evaluator.getPackage(psiClass)?.qualifiedName == classPackage
}

