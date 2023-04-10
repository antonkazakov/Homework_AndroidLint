package ru.otus.homework.lintchecks

import com.android.tools.lint.detector.api.*
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.kotlin.KotlinUBinaryExpression

class JobDetector : Detector(), Detector.UastScanner {

    companion object {

        private const val ID = "JobInBuilderUsage"
        private const val BRIEF_DESCRIPTION = "Job / Supervisor Job isn`t allowed"
        private const val EXPLANATION = "Using this considered as anti-pattern"
        private const val COROUTINE_SCOPE_CLASS = "kotlinx.coroutines.CoroutineScope"
        private const val ANDROIDX_VIEW_MODEL_CLASS = "androidx.lifecycle.ViewModel"
        private const val COROUTINE_CONTEXT = "kotlin.coroutines.CoroutineContext"
        private const val COROUTINE_JOB = "kotlinx.coroutines.Job"
        private const val COROUTINE_NON_CANCELABLE = "kotlinx.coroutines.NonCancellable"
        private const val COROUTINE_SUPERVISOR_JOB = "kotlinx.coroutines.CompletableJob"
        private const val COROUTINE_WITH_CONTEXT = "withContext"
        private const val ANDROIDX_VIEWMODEL_KTX = "androidx.lifecycle:lifecycle-viewmodel-kt"


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

    override fun getApplicableMethodNames(): List<String> = listOf("launch", "async")


    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {

        if (context.evaluator.getTypeClass(node.receiverType)
                ?.hasClass(context, COROUTINE_SCOPE_CLASS) == false
        ) return

        for (arg in node.valueArguments) {
            val param = context.evaluator.getTypeClass(arg.getExpressionType())
            var hasJob = context.evaluator.inheritsFrom(param, COROUTINE_JOB, false)

            if (!hasJob && arg is KotlinUBinaryExpression) {
                hasJob = context.evaluator.inheritsFrom(
                    param,
                    COROUTINE_CONTEXT,
                    false
                )
            }

            if (!hasJob) continue

            context.report(
                issue = ISSUE,
                scope = node,
                location = context.getLocation(arg),
                message = BRIEF_DESCRIPTION,
                quickfixData = createFix(context, arg, node)
            )
        }
    }

    private fun createFix(
        context: JavaContext,
        node: UExpression,
        parentNode: UExpression
    ): LintFix? = when {
        node.hasClass(context, ANDROIDX_VIEW_MODEL_CLASS) &&
                context.inheritsFrom(COROUTINE_SUPERVISOR_JOB, node)
        -> createSupervisorJobFix(context, node)

        node is KotlinUBinaryExpression && context.inheritsFrom(COROUTINE_CONTEXT, node) -> {
            for (expression in node.operands) {
                val isSupervisorJobExpr = context.inheritsFrom(COROUTINE_SUPERVISOR_JOB, expression)
                if (isSupervisorJobExpr) createSupervisorJobFix(context, expression)
            }
            null
        }

        context.inheritsFrom(COROUTINE_NON_CANCELABLE, node) -> {
            createNonCancelableJobFix(context, parentNode)
        }

        else -> null
    }

    private fun createSupervisorJobFix(
        context: JavaContext,
        node: UExpression
    ): LintFix? = context.getLintModelLibrary(ANDROIDX_VIEWMODEL_KTX)?.let {
        fix()
            .replace()
            .range(context.getLocation(node))
            .all()
            .with("")
            .build()
    }

    private fun createNonCancelableJobFix(
        context: JavaContext,
        node: UExpression
    ): LintFix? = if (node.isInsideCoroutine(getApplicableMethodNames())) {
        fix()
            .replace()
            .range(context.getLocation(node))
            .text("launch")
            .with(COROUTINE_WITH_CONTEXT)
            .build()
    } else null
}