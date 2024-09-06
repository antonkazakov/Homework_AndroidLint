package ru.otus.homework.lintchecks

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.uast.UElement
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.getContainingUClass

@Suppress("UnstableApiUsage")
class GlobalScopeUsageDetector : Detector(), SourceCodeScanner {

    companion object {
        private const val ANDROIDX_LIFECYCLE_VIEW_MODEL = "androidx.lifecycle.ViewModel"
        private const val ANDROIDX_FRAGMENT_APP_FRAGMENT = "androidx.fragment.app.Fragment"
        private const val ANDROIDX_LIFECYCLE_LIFECYCLE_VIEW_MODEL_KTX = "androidx.lifecycle:lifecycle-viewmodel-ktx"
        private const val ANDROIDX_LIFECYCLE_LIFECYCLE_RUNTIME_KTX = "androidx.lifecycle:lifecycle-runtime-ktx"

        private const val VIEW_MODEL_SCOPE = "viewModelScope"
        private const val VIEW_LIFECYCLE_OWNER_LIFECYCLE_SCOPE = "viewLifecycleOwner.lifecycleScope"

        private const val GLOBAL_SCOPE = "GlobalScope"
        private const val ID = "GlobalScopeUsage"
        private const val BRIEF_DESCRIPTION = "Avoid using GlobalScope for coroutines"
        private const val EXPLANATION = """
                Using GlobalScope for coroutines can lead to resource leaks and uncontrollable background operations.
                Instead, use viewModelScope in ViewModel classes or lifecycleScope in Fragment classes.
            """

        val ISSUE = Issue.create(
            id = ID,
            briefDescription = BRIEF_DESCRIPTION,
            explanation = EXPLANATION,
            category = Category.CORRECTNESS,
            priority = 5,
            severity = Severity.WARNING,
            implementation = Implementation(GlobalScopeUsageDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }

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
                        quickfixData = quickFix(context, node)
                    )
                }
            }
        }
    }

    private fun quickFix(
        context: JavaContext,
        node: USimpleNameReferenceExpression
    ): LintFix? {
        val psiClass = node.getContainingUClass()?.javaPsi

        if (psiClass?.hasParent(context, ANDROIDX_LIFECYCLE_VIEW_MODEL) == true) {
            return quickViewModelScopeFix(
                context, node
            )
        }

        if (psiClass?.hasParent(context, ANDROIDX_FRAGMENT_APP_FRAGMENT) == true) {
            return quickFragmentScopeFix(context, node)
        }

        return null
    }

    private fun quickViewModelScopeFix(
        context: JavaContext,
        node: USimpleNameReferenceExpression
    ): LintFix? {
        val hasViewModelArtifact = hasArtifact(
            context = context,
            artifactName = ANDROIDX_LIFECYCLE_LIFECYCLE_VIEW_MODEL_KTX
        )

        if (!hasViewModelArtifact) return null

        return fix()
            .replace()
            .range(context.getLocation(node))
            .with(VIEW_MODEL_SCOPE)
            .shortenNames()
            .reformat(true)
            .build()
    }

    private fun quickFragmentScopeFix(
        context: JavaContext,
        node: USimpleNameReferenceExpression
    ): LintFix? {
        val hasFragmentArtifact = hasArtifact(
            context = context,
            artifactName = ANDROIDX_LIFECYCLE_LIFECYCLE_RUNTIME_KTX
        )

        if (!hasFragmentArtifact) return null

        return fix()
            .replace()
            .range(context.getLocation(node))
            .with(VIEW_LIFECYCLE_OWNER_LIFECYCLE_SCOPE)
            .shortenNames()
            .reformat(true)
            .build()
    }
}