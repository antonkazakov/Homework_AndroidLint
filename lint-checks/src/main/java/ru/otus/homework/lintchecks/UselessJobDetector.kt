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
import org.jetbrains.uast.getContainingUClass

@Suppress("UnstableApiUsage")
class UselessJobDetector: Detector(), Detector.UastScanner {

    override fun getApplicableMethodNames(): List<String> = listOf("launch", "async")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {

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

        val evaluator = context.evaluator

        val isNonCancelableJob = evaluator.inheritsFrom(
            evaluator.getTypeClass(node.getExpressionType()),
            "kotlinx.coroutines.NonCancellable",
            false
        )

        val isSupervisorJob = evaluator.inheritsFrom(
            evaluator.getTypeClass(node.getExpressionType()),
            "kotlinx.coroutines.Job",
            false
        )

        val calledInClas = node.getContainingUClass()?.javaPsi
        val isInViewModel = calledInClas?.qualifiedName == "androidx.lifecycle.ViewModel"

        val fix = when{
            isNonCancelableJob -> supervisorJobFix(context, node)
            isSupervisorJob && isInViewModel -> nonCancellableJobFix(context, node)
            else -> null
        }

        context.report(
            issue = ISSUE,
            scope = node,
            location = context.getLocation(node),
            message = "Using Job/SupervisorJob in a builder makes no sense",
            quickfixData = fix
        )
    }

    private fun supervisorJobFix(context: JavaContext, node: UCallExpression): LintFix {
        return fix().replace().range(context.getLocation(node)).text(node.methodName).with("withContext").build()
    }

    private fun nonCancellableJobFix(context: JavaContext, node: UCallExpression): LintFix {
        return fix().replace().range(context.getLocation(node)).all().with("").build()
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