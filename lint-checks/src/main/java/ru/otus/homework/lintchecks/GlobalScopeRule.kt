package ru.otus.homework.lintchecks

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import org.jetbrains.uast.UElement
import org.jetbrains.uast.USimpleNameReferenceExpression

@Suppress("UnstableApiUsage")
class GlobalScopeRule : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> {
        return listOf(USimpleNameReferenceExpression::class.java)
    }

    override fun createUastHandler(
        context: JavaContext
    ): UElementHandler {
        return object : UElementHandler() {
            override fun visitSimpleNameReferenceExpression(node: USimpleNameReferenceExpression) {
                if (node.identifier != GLOBAL_SCOPE_NODE_IDENTIFIER) return

                context.report(
                    issue = ISSUE,
                    scope = node,
                    location = context.getLocation(node),
                    message = DESCRIPTION,
                    quickfixData = getQuickFix(context, node)
                )
            }
        }
    }

    private fun getQuickFix(
        context: JavaContext,
        node: USimpleNameReferenceExpression
    ) = when {
            isViewModelContext(context, node) -> suggestReplaceScope("viewModelScope")
            isFragmentContext(context, node) -> suggestReplaceScope("lifecycleScope")
            else -> null
        }

    private fun suggestReplaceScope(
        recommendedScope: String
    ) = fix().replace()
            .text(GLOBAL_SCOPE_NODE_IDENTIFIER)
            .with(recommendedScope)
            .build()

    private fun isViewModelContext(
        context: JavaContext,
        node: USimpleNameReferenceExpression
    ) = node.hasClass(context, "androidx.lifecycle.ViewModel")
                && hasArtifact(context, "androidx.lifecycle:lifecycle-viewmodel-ktx")

    private fun isFragmentContext(
        context: JavaContext,
        node: USimpleNameReferenceExpression
    ) = node.hasClass(context, "androidx.fragment.app.Fragment")
                && hasArtifact(context, "androidx.lifecycle:lifecycle-runtime-ktx")

    private fun hasArtifact(context: JavaContext, artifactName: String): Boolean {
        val dependenciesList = context.evaluator.dependencies ?: return false

        return dependenciesList.getAll().any { it.identifier.contains(artifactName) }
    }

    companion object {

        private const val DESCRIPTION = "GlobalScope usage is not recommended"
        private const val GLOBAL_SCOPE_NODE_IDENTIFIER = "GlobalScope"

        val ISSUE = Issue.create(
            id = "GlobalScope",
            briefDescription = DESCRIPTION,
            explanation = "GlobalScope require manual track lifecycle. It's can cause difficult debuggable problems",
            category = Category.CUSTOM_LINT_CHECKS,
            priority = 5,
            severity = Severity.WARNING,
            implementation = Implementation(GlobalScopeRule::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }
}
