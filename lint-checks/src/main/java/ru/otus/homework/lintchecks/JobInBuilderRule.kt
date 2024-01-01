package ru.otus.homework.lintchecks


import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.kotlin.KotlinUBinaryExpression

@Suppress("UnstableApiUsage")
class JobInBuilderRule : Detector(), Detector.UastScanner {

    override fun getApplicableMethodNames(): List<String> {
        return listOf("launch", "async")
    }

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val methodPerformOnClass = context.evaluator.getTypeClass(node.receiverType) ?: return

        if (!methodPerformOnClass.hasClass(context, "kotlinx.coroutines.CoroutineScope")) return

        node.valueArguments.forEach { argument ->
            val hasPassedJob = isInheritsFromClass(
                context, argument, "kotlinx.coroutines.Job"
            ) || (argument is KotlinUBinaryExpression && isInheritsFromClass(
                context, node, "kotlin.coroutines.CoroutineContext"
            ))

            if (!hasPassedJob) return@forEach

            context.report(
                issue = ISSUE,
                scope = node,
                location = context.getLocation(argument),
                message = DESCRIPTION,
                quickfixData = getQuickFix(context, node)
            )
        }
    }

    private fun getQuickFix(
        context: JavaContext,
        node: UCallExpression
    ) = when {
            isNonCancellableJob(context, node) -> suggestReplaceCall(context, node)
            isJobInViewModel(context, node) -> suggestRemoveJob(context, node)
            else -> null
        }

    private fun suggestRemoveJob(
        context: JavaContext,
        node: UExpression
    ) = fix().replace()
            .range(context.getLocation(node)).all()
            .with("")
            .build()

    private fun suggestReplaceCall(
        context: JavaContext,
        node: UCallExpression
    ) = fix().replace()
            .range(context.getLocation(node))
            .text(node.methodName)
            .with( "withContext")
            .build()


    private fun isJobInViewModel(
        context: JavaContext,
        node: UCallExpression
    ): Boolean {
        val isSupervisorJob = isInheritsFromClass(
            context, node, "kotlinx.coroutines.CompletableJob"
        )

        val isViewModelContext = node.hasClass(context, "androidx.lifecycle.ViewModel")

        return isSupervisorJob && isViewModelContext
    }

    private fun isNonCancellableJob(
        context: JavaContext,
        node: UCallExpression
    ) = isInheritsFromClass(
            context, node, "kotlinx.coroutines.NonCancellable"
        )

    private fun isInheritsFromClass(
        context: JavaContext,
        node: UExpression,
        className: String
    ) = with(context.evaluator) {
            if (node is KotlinUBinaryExpression) {
                node.operands.any { inheritsFrom(getTypeClass(it.getExpressionType()), className) }
            } else {
                inheritsFrom(getTypeClass(node.getExpressionType()), className)
            }
        }

    companion object {

        private const val DESCRIPTION = "Don`t pass Job/SupervisorJob in coroutine builder"

        val ISSUE = Issue.create(
            id = "JobInBuilder",
            briefDescription = DESCRIPTION,
            explanation = "Passing Job/SupervisorJob into coroutine builder will provide errors",
            category = Category.CUSTOM_LINT_CHECKS,
            priority = 5,
            severity = Severity.ERROR,
            implementation = Implementation(
                JobInBuilderRule::class.java, Scope.JAVA_FILE_SCOPE
            )
        )
    }
}
