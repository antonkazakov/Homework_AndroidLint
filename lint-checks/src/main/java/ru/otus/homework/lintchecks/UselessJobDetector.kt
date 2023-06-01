package ru.otus.homework.lintchecks

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.USimpleNameReferenceExpression

@Suppress("UnstableApiUsage")
class UselessJobDetector: Detector(), Detector.UastScanner {

    override fun getApplicableMethodNames(): List<String> = listOf("launch", "async")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        //super.visitMethodCall(context, node, method)

        node.valueArguments.forEach { argument ->

            val jobInBuilder = context.evaluator.inheritsFrom(
                context.evaluator.getTypeClass(argument.getExpressionType()),
                "kotlinx.coroutines.Job",
                false
            )

            if(jobInBuilder){



                reportUsage(context, node, method)
            }

        }

    }

    private fun reportUsage(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        context.report(
            issue = ISSUE,
            scope = node,
            location = context.getLocation(node),
            message = "Using Job/SupervisorJob in a builder makes no sense",
            //quickfixData = fix
        )
    }

    companion object {

        private val IMPLEMENTATION = Implementation(
            UselessJobDetector::class.java,
            Scope.JAVA_FILE_SCOPE
        )

        val ISSUE: Issue = Issue
            .create(
                id = "JobInBuilderUsage",
                briefDescription = "The Job in a coroutine builder should not be used",
                explanation = """
                Using Job/SupervisorJob in a builder makes no sense.
            """.trimIndent(),
                category = Category.CORRECTNESS,
                priority = 10,
                severity = Severity.ERROR,
                androidSpecific = true,
                implementation = IMPLEMENTATION
            )
    }

}