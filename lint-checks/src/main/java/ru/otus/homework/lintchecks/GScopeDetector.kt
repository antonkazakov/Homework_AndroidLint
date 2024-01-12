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


class GScopeDetector : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes() = listOf(USimpleNameReferenceExpression::class.java)

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {
        override fun visitSimpleNameReferenceExpression(node: USimpleNameReferenceExpression) {
            if (node.identifier != GLOBAL_SCOPE) return

            context.report(
                issue = ISSUE,
                scope = node,
                location = context.getLocation(node),
                message = BRIEF_DESCRIPTION,
            )
        }
    }

    companion object {

        private const val ID = "GlobalScopeUsage"
        private const val BRIEF_DESCRIPTION = "Использовать GlobalScopeUsage не рекомендуется"
        private const val EXPLANATION = "Подробное описание"

        private const val PRIORITY = 6

        private const val GLOBAL_SCOPE = "GlobalScope"

        val ISSUE = Issue.create(
            ID,
            BRIEF_DESCRIPTION,
            EXPLANATION,
            Category.create("TEST CATEGORY", 10),
            PRIORITY,
            Severity.WARNING,
            Implementation(GScopeDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }

}