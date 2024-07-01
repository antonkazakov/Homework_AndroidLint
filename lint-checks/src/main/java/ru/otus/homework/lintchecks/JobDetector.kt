@file:Suppress("UnstableApiUsage")

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
import org.jetbrains.uast.UExpression

class JobDetector : Detector(), Detector.UastScanner {

    override fun getApplicableMethodNames(): List<String> = listOf("launch", "async")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (isNotCoroutineScope(context, node)) return

        for (arg in node.valueArguments) {
            val param = context.evaluator.getTypeClass(arg.getExpressionType())
            val isJob = context.evaluator.inheritsFrom(param, COROUTINE_JOB, false)
            val isContext = context.evaluator.inheritsFrom(param, COROUTINE_CONTEXT, false)
            if (!isJob && !isContext) continue

            context.report(
                issue = ISSUE,
                scope = node,
                location = context.getLocation(arg),
                message = BRIEF_DESCRIPTION,
                quickfixData = createFix(context, arg, node)
            )
        }
    }

    private fun isNotCoroutineScope(
        context: JavaContext,
        node: UCallExpression
    ) = context.evaluator.getTypeClass(node.receiverType)
        ?.isHasParent(context, COROUTINE_SCOPE_CLASS) == false

    private fun createFix(
        context: JavaContext,
        node: UExpression,
        parentNode: UCallExpression
    ): LintFix? = when {
        isSupervisorJobInViewModel(context, node) -> suggestDeleteJob(context, node)
        isNonCancellableRunJob(context, node) -> suggestReplaceCall(context, parentNode)
        else -> null
    }

    private fun isSupervisorJob(context: JavaContext, node: UExpression) =
        context.inheritsFrom(COROUTINE_SUPERVISOR_JOB, node)

    private fun isSupervisorJobInViewModel(context: JavaContext, node: UExpression) =
        node.isHasParent(ANDROIDX_VIEW_MODEL_CLASS) && isSupervisorJob(context, node)

    private fun isNonCancellableRunJob(context: JavaContext, node: UExpression) =
        context.inheritsFrom(COROUTINE_NON_CANCELABLE, node) && node.isInsideCoroutine(
            getApplicableMethodNames()
        )

    private fun suggestDeleteJob(
        context: JavaContext,
        node: UExpression
    ): LintFix = fix().replace()
            .range(context.getLocation(node))
            .all()
            .with("")
            .build()

    private fun suggestReplaceCall(
        context: JavaContext,
        node: UCallExpression
    ) = fix().replace()
        .range(context.getLocation(node))
        .text(node.methodName)
        .with(COROUTINE_WITH_CONTEXT)
        .build()

    companion object {

        private const val ID = "JobInBuilderUsage"
        private const val BRIEF_DESCRIPTION =
            "Job / Supervisor Job не допускается в корутин-билдере"
        private const val EXPLANATION =
            "Использование Job внутри корутин-билдеров не имеет никакого эффекта, это может сломать ожидаемые обработку ошибок и механизм отмены корутин."
        private const val COROUTINE_SCOPE_CLASS = "kotlinx.coroutines.CoroutineScope"
        private const val ANDROIDX_VIEW_MODEL_CLASS = "androidx.lifecycle.ViewModel"
        private const val COROUTINE_CONTEXT = "kotlin.coroutines.CoroutineContext"
        private const val COROUTINE_JOB = "kotlinx.coroutines.Job"
        private const val COROUTINE_NON_CANCELABLE = "kotlinx.coroutines.NonCancellable"
        private const val COROUTINE_SUPERVISOR_JOB = "kotlinx.coroutines.CompletableJob"
        private const val COROUTINE_WITH_CONTEXT = "withContext"

        val ISSUE = Issue.create(
            id = ID,
            briefDescription = BRIEF_DESCRIPTION,
            explanation = EXPLANATION,
            category = Category.LINT,
            priority = 7,
            severity = Severity.ERROR,
            implementation = Implementation(
                JobDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }
}