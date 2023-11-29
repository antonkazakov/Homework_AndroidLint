package ru.otus.homework.lintchecks.rules

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Position
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.kotlin.KotlinUBinaryExpression
import ru.otus.homework.lintchecks.hasClass

@Suppress("UnstableApiUsage")
class JobInBuilderUsageRule : Detector(), Detector.UastScanner {

    companion object {
        val ISSUE = Issue.create(
            id = RULE_ID,
            briefDescription = RULE_DESCRIPTION,
            explanation = RULE_EXPLANATION,
            category = Category.CUSTOM_LINT_CHECKS,
            priority = RULE_PRIORITY,
            severity = Severity.ERROR,
            implementation = Implementation(
                JobInBuilderUsageRule::class.java, Scope.JAVA_FILE_SCOPE
            )
        )
    }

    override fun getApplicableMethodNames(): List<String>? {
        return listOf(LAUNCH_METHOD, ASYNC_METHOD)
    }

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val methodPerformOnClass = context.evaluator.getTypeClass(node.receiverType) ?: return

        if (!methodPerformOnClass.hasClass(context, COROUTINE_SCOPE_CLASS)) return

        node.valueArguments.forEach { argument ->
            val hasPassedJob = isInheritsFromClass(
                context, argument, COROUTINE_JOB_CLASS
            ) || (argument is KotlinUBinaryExpression && isInheritsFromClass(
                context, node, COROUTINE_CONTEXT_CLASS
            ))

            if (!hasPassedJob) return@forEach

            context.report(
                issue = ISSUE,
                scope = node,
                location = context.getLocation(argument),
                message = RULE_DESCRIPTION,
                quickfixData = getQuickFix(context, node)
            )
        }
    }

    private fun getQuickFix(
        context: JavaContext,
        node: UCallExpression
    ): LintFix? {
        return when {
            isNonCancellableJob(context, node) -> suggestReplaceCallToWithContext(context, node)
            isSupervisorJobInViewModel(context, node) -> suggestRemoveSuspendJob(context, node)
            else -> null
        }
    }

    private fun suggestRemoveSuspendJob(
        context: JavaContext,
        node: UExpression
    ): LintFix {
        return fix().replace()
            .range(context.getLocation(node)).all()
            .with(EMPTY_STRING)
            .build()
    }

    private fun suggestReplaceCallToWithContext(
        context: JavaContext,
        node: UCallExpression
    ): LintFix {
        return fix().replace()
            .range(context.getLocation(node))
            .text(node.methodName)
            .with(WITH_CONTEXT_METHOD)
            .build()
    }


    private fun isSupervisorJobInViewModel(
        context: JavaContext,
        node: UCallExpression
    ): Boolean {
        val isSupervisorJob = isInheritsFromClass(
            context, node, COROUTINE_COMPLETABLE_JOB_CLASS
        )

        val isViewModelContext = node.hasClass(context, LIFECYCLE_VIEWMODEL_CLASS)

        return isSupervisorJob && isViewModelContext
    }

    private fun isNonCancellableJob(
        context: JavaContext,
        node: UCallExpression
    ): Boolean {
        return isInheritsFromClass(
            context, node, NON_CANCELLABLE_CLASS
        )
    }

    private fun isInheritsFromClass(
        context: JavaContext,
        node: UExpression,
        className: String
    ): Boolean {
        return with(context.evaluator) {
            if (node is KotlinUBinaryExpression) {
                node.operands.any { inheritsFrom(getTypeClass(it.getExpressionType()), className) }
            } else {
                inheritsFrom(getTypeClass(node.getExpressionType()), className)
            }
        }
    }
}

private const val RULE_ID = "JobInBuilderUsage"
private const val RULE_DESCRIPTION = "Job/SupervisorJob is not allowed passing in coroutine builder"
private const val RULE_EXPLANATION =
    "Passing Job/SupervisorJob into coroutine builder may broken catching error and job cancelling mechanism"
private const val RULE_PRIORITY = 2

private const val LIFECYCLE_VIEWMODEL_CLASS = "androidx.lifecycle.ViewModel"
private const val COROUTINE_SCOPE_CLASS = "kotlinx.coroutines.CoroutineScope"
private const val COROUTINE_CONTEXT_CLASS = "kotlin.coroutines.CoroutineContext"
private const val COROUTINE_JOB_CLASS = "kotlinx.coroutines.Job"
private const val COROUTINE_COMPLETABLE_JOB_CLASS = "kotlinx.coroutines.CompletableJob"
private const val NON_CANCELLABLE_CLASS = "kotlinx.coroutines.NonCancellable"

private const val WITH_CONTEXT_METHOD = "withContext"

private const val LAUNCH_METHOD = "launch"
private const val ASYNC_METHOD = "async"

private const val EMPTY_STRING = ""
