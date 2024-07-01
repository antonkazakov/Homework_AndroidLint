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
import org.jetbrains.uast.UElement
import org.jetbrains.uast.USimpleNameReferenceExpression

@Suppress("UnstableApiUsage")
class GlobalScopeDetector : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(
        USimpleNameReferenceExpression::class.java
    )

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {
            override fun visitSimpleNameReferenceExpression(node: USimpleNameReferenceExpression) {
                if (node.identifier == GLOBAL_SCOPE) {
                    context.report(
                        issue = ISSUE,
                        message = BRIEF_DESCRIPTION,
                        scope = node,
                        location = context.getLocation(node),
                        quickfixData = createFix(context, node)
                    )
                }
            }
        }

    private fun createFix(
        context: JavaContext,
        node: USimpleNameReferenceExpression
    ): LintFix? {
        if (node.isHasParent(VIEWMODEL_CLASS) && context.isHasDependency(VIEWMODEL_ARTIFACT)) {
            return createFix(VIEWMODEL_FIX)
        }

        if (node.isHasParent(FRAGMENT_CLASS) && context.isHasDependency(LIFECYCLE_ARTIFACT)) {
            return createFix(FRAGMENT_FIX)
        }

        return null
    }

    private fun createFix(
        replaceWithText: String
    ): LintFix = fix()
        .replace()
        .text(GLOBAL_SCOPE)
        .with(replaceWithText)
        .shortenNames()
        .reformat(true)
        .build()

    companion object {
        private const val ID = "GlobalScopeUsage"
        private const val BRIEF_DESCRIPTION = "Использование GlobalScope не допускается"
        private const val EXPLANATION = "Замените на viewModelScope или lifecycleScope"
        private const val GLOBAL_SCOPE = "GlobalScope"
        private const val VIEWMODEL_ARTIFACT = "androidx.lifecycle:lifecycle-viewmodel-ktx"
        private const val VIEWMODEL_CLASS = "androidx.lifecycle.ViewModel"
        private const val VIEWMODEL_FIX = "viewModelScope"
        private const val LIFECYCLE_ARTIFACT = "androidx.lifecycle:lifecycle-runtime-ktx"
        private const val FRAGMENT_CLASS = "androidx.fragment.app.Fragment"
        private const val FRAGMENT_FIX = "lifecycleScope"

        val ISSUE = Issue.create(
            id = ID,
            briefDescription = BRIEF_DESCRIPTION,
            explanation = EXPLANATION,
            category = Category.LINT,
            priority = 7,
            severity = Severity.ERROR,
            implementation = Implementation(GlobalScopeDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }
}