package ru.otus.homework.lintchecks.rules

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import org.jetbrains.uast.UElement
import org.jetbrains.uast.USimpleNameReferenceExpression
import ru.otus.homework.lintchecks.hasArtifact
import ru.otus.homework.lintchecks.hasClass

@Suppress("UnstableApiUsage")
class GlobalScopeUsageRule : Detector(), Detector.UastScanner {
    companion object {
        val ISSUE = Issue.create(
            id = RULE_ID,
            briefDescription = RULE_DESCRIPTION,
            explanation = RULE_EXPLANATION,
            category = Category.CUSTOM_LINT_CHECKS,
            priority = RULE_PRIORITY,
            severity = Severity.WARNING,
            implementation = Implementation(GlobalScopeUsageRule::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }

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
                    message = RULE_DESCRIPTION,
                    quickfixData = getQuickFix(context, node)
                )
            }
        }
    }

    private fun getQuickFix(
        context: JavaContext,
        node: USimpleNameReferenceExpression
    ): LintFix? {
        return when {
            isViewModelContext(context, node) -> suggestReplaceScope(VIEWMODEL_SCOPE)
            isFragmentContext(context, node) -> suggestReplaceScope(LIFECYCLE_SCOPE)
            else -> null
        }
    }

    private fun suggestReplaceScope(
        recommendedScope: String
    ): LintFix {
        return fix().replace()
            .text(GLOBAL_SCOPE_NODE_IDENTIFIER)
            .with(recommendedScope)
            .build()
    }

    private fun isViewModelContext(
        context: JavaContext,
        node: USimpleNameReferenceExpression
    ): Boolean {
        return node.hasClass(context, LIFECYCLE_VIEWMODEL_CLASS)
                && hasArtifact(context, LIFECYCLE_VIEWMODEL_ARTIFACT)
    }

    private fun isFragmentContext(
        context: JavaContext,
        node: USimpleNameReferenceExpression
    ): Boolean {
        return node.hasClass(context, ANDROIDX_FRAGMENT_CLASS)
                && hasArtifact(context, LIFECYCLE_RUNTIME_ARTIFACT)
    }
}

private const val GLOBAL_SCOPE_ISSUE_DETAILS_LINK =
    "https://elizarov.medium.com/the-reason-to-avoid-globalscope-835337445abc"

private const val RULE_ID = "GlobalScopeUsage"
private const val RULE_DESCRIPTION = "GlobalScope usage is not recommended"
private const val RULE_EXPLANATION =
    "GlobalScope require manual track lifecycle. It's can cause difficult debuggable problems. " +
            "For details read: $GLOBAL_SCOPE_ISSUE_DETAILS_LINK"
private const val RULE_PRIORITY = 5

private const val LIFECYCLE_VIEWMODEL_CLASS = "androidx.lifecycle.ViewModel"
private const val ANDROIDX_FRAGMENT_CLASS = "androidx.fragment.app.Fragment"

private const val LIFECYCLE_VIEWMODEL_ARTIFACT = "androidx.lifecycle:lifecycle-viewmodel-ktx"
private const val LIFECYCLE_RUNTIME_ARTIFACT = "androidx.lifecycle:lifecycle-runtime-ktx"

private const val LIFECYCLE_SCOPE = "lifecycleScope"
private const val VIEWMODEL_SCOPE = "viewModelScope"

private const val GLOBAL_SCOPE_NODE_IDENTIFIER = "GlobalScope"
