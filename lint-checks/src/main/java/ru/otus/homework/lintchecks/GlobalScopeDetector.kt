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

private const val ID = "GlobalScopeUsage"
private const val BRIEF_DESCRIPTION = "GlobalScope usage isn`t allowed"
private const val EXPLANATION = "Using GlobalScope considered as anti-pattern"
private const val GLOBAL_SCOPE = "GlobalScope"
private const val ANDROIDX_VIEWMODEL_ARTIFACT = "androidx.lifecycle:lifecycle-viewmodel-ktx"
private const val ANDROIDX_VIEWMODEL_CLASS = "androidx.lifecycle.ViewModel"
private const val ANDROIDX_VIEWMODEL_FIX = "viewModelScope"
private const val ANDROIDX_LIFECYCLE_ARTIFACT = "androidx.lifecycle:lifecycle-runtime-ktx"
private const val ANDROIDX_FRAGMENT_CLASS = "androidx.fragment.app.Fragment"
private const val ANDROIDX_FRAGMENT_FIX = "lifecycleScope"

@Suppress("UnstableApiUsage")
class GlobalScopeDetector : Detector(), Detector.UastScanner {

    companion object {
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

    override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(
        USimpleNameReferenceExpression::class.java
    )

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {
            override fun visitSimpleNameReferenceExpression(node: USimpleNameReferenceExpression) {
                if (node.identifier != GLOBAL_SCOPE)
                    return

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
        when {
            node.hasClass(context, ANDROIDX_VIEWMODEL_CLASS) -> return createScopeFix(
                context = context,
                artifact = ANDROIDX_VIEWMODEL_ARTIFACT,
                fix = ANDROIDX_VIEWMODEL_FIX
            )

            node.hasClass(context, ANDROIDX_FRAGMENT_CLASS) -> return createScopeFix(
                context = context,
                artifact = ANDROIDX_LIFECYCLE_ARTIFACT,
                fix = ANDROIDX_FRAGMENT_FIX
            )
        }
        return null
    }

    private fun createScopeFix(
        context: JavaContext,
        artifact: String,
        fix: String
    ): LintFix? {
        return context.getLintModelLibrary(artifact)
            ?.let {
                fix()
                    .replace()
                    .text(GLOBAL_SCOPE)
                    .with(fix)
                    .shortenNames()
                    .reformat(true)
                    .build()
            }
    }
}