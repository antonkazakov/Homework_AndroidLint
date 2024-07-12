package ru.otus.homework.lintchecks.detectors


import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import org.jetbrains.uast.USimpleNameReferenceExpression
import ru.otus.homework.lintchecks.LintConstants.ISSUE_CATEGORY_NAME
import ru.otus.homework.lintchecks.LintConstants.ISSUE_MAX_PRIORITY

@Suppress("UnstableApiUsage")
class GlobalScopeUsageDetector : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes() = listOf(USimpleNameReferenceExpression::class.java)

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {
        override fun visitSimpleNameReferenceExpression(node: USimpleNameReferenceExpression) {
            if (node.identifier == GLOBAL_SCOPE) {
                context.report(
                    issue = ISSUE,
                    scope = node,
                    location = context.getLocation(node),
                    message = BRIEF_DESCRIPTION,
                )
            }
        }
    }

    companion object {

        private const val ID = "GlobalScopeUsage"
        private const val BRIEF_DESCRIPTION = "Использовать GlobalScopeUsage не рекомендуется"
        private const val EXPLANATION =
            "Корутины, запущенные на kotlinx.coroutines.GlobalScope нужно контролировать вне скоупа класс, в котором они созданы. Контролировать глобальные корутины неудобно, а отсутствие контроля может привести к излишнему использованию ресурсов и утечкам памяти."

        private const val PRIORITY = 6

        private const val GLOBAL_SCOPE = "GlobalScope"

        val ISSUE = Issue.create(
            id = ID,
            briefDescription = BRIEF_DESCRIPTION,
            explanation = EXPLANATION,
            category = Category.create(ISSUE_CATEGORY_NAME, ISSUE_MAX_PRIORITY),
            priority = PRIORITY,
            severity = Severity.WARNING,
            implementation = Implementation(
                GlobalScopeUsageDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }
}