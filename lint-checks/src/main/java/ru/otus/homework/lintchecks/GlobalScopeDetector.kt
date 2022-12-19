package ru.otus.homework.lintchecks

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.getContainingUClass

private const val ID = "GlobalScopeUsage"
private const val BRIEF_DESCRIPTION = "Описание что не так"
private const val EXPLANATION = "Полное описание проблемы"
private const val PRIORITY = 5

private const val GLOBAL_SCOPE = "GlobalScope"
private const val VIEWMODEL = "androidx.lifecycle.ViewModel"
private const val VIEWMODEL_ARTIFACT = "androidx.lifecycle:lifecycle-viewmodel-ktx"
private const val VIEWMODEL_SCOPE = "viewModelScope"
private const val FRAGMENT = "androidx.fragment.app.Fragment"
private const val LIFECYCLE_ARTIFACT = "androidx.lifecycle:lifecycle-runtime-ktx"
private const val LIFECYCLE_SCOPE = "lifecycleScope"

@Suppress("UnstableApiUsage")
class GlobalScopeDetector : Detector(), Detector.UastScanner {

    companion object {
        val ISSUE = Issue.create(
            ID,
            BRIEF_DESCRIPTION,
            EXPLANATION,
            Category.create("HOMEWORK CATEGORY", 10),
            PRIORITY,
            Severity.WARNING,
            Implementation(GlobalScopeDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }

    override fun getApplicableUastTypes() = listOf(USimpleNameReferenceExpression::class.java)

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {

        override fun visitSimpleNameReferenceExpression(node: USimpleNameReferenceExpression) {
            if (node.identifier != GLOBAL_SCOPE) return
            val clazz = node.getContainingUClass() ?: return

            if (context.hasDependencies(VIEWMODEL_ARTIFACT) && clazz.hasSuperClass(VIEWMODEL)) {
                context.reportAndFix(node, VIEWMODEL_SCOPE)
            }

            if (context.hasDependencies(LIFECYCLE_ARTIFACT) && clazz.hasSuperClass(FRAGMENT)) {
                context.reportAndFix(node, LIFECYCLE_SCOPE)
            }
        }
    }

    private fun createFix(fix: String) = fix().replace().text(GLOBAL_SCOPE).with(fix).build()

    private fun JavaContext.reportAndFix(node: USimpleNameReferenceExpression, fix: String) {
        report(
            issue = ISSUE,
            message = BRIEF_DESCRIPTION,
            scope = node,
            location = getLocation(node),
            quickfixData = createFix(fix)
        )
    }

}