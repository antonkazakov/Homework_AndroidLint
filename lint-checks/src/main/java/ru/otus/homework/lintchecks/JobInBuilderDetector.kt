package ru.otus.homework.lintchecks

import com.android.tools.lint.detector.api.*
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.kotlin.KotlinUBinaryExpression

private const val ID = "JobInBuilderUsage"
private const val BRIEF_DESCRIPTION = "Job / Supervisor Job is not allowed"
private const val EXPLANATION = "Using this is considered as anti-pattern"

private const val METHOD_ASYNC = "async"
private const val METHOD_LAUNCH = "launch"

private const val COROUTINE_SCOPE_CLASS = "kotlinx.coroutines.CoroutineScope"
private const val ANDROIDX_VIEWMODEL_CLASS = "androidx.lifecycle.ViewModel"

private const val COROUTINE_CONTEXT = "kotlin.coroutines.CoroutineContext"
private const val COROUTINE_JOB = "kotlinx.coroutines.Job"
private const val COROUTINE_NON_CANCELABLE = "kotlinx.coroutines.NonCancellable"
private const val COROUTINE_SUPERVISOR_JOB = "kotlinx.coroutines.CompletableJob"
private const val COROUTINE_WITH_CONTEXT = "withContext"

private const val ANDROIDX_VIEWMODEL_KTX = "androidx.lifecycle:lifecycle-viewmodel-kt"

private const val EMPTY = ""

@Suppress("UnstableApiUsage")
class JobDetector : Detector(), Detector.UastScanner {

    companion object {
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

    override fun getApplicableMethodNames(): List<String> {
        return listOf(
            METHOD_ASYNC,
            METHOD_LAUNCH
        )
    }

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (context.evaluator
                .getTypeClass(node.receiverType)
                ?.hasClass(context, COROUTINE_SCOPE_CLASS) == false
        )
            return

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
    ): LintFix? {
        return when {
            node.hasClass(context, ANDROIDX_VIEWMODEL_CLASS) && context.inheritsFrom(
                COROUTINE_SUPERVISOR_JOB,
                node
            ) -> {
                return createSupervisorJobFix(context, node)
            }
            node is KotlinUBinaryExpression && context.inheritsFrom(COROUTINE_CONTEXT, node) -> {
                for (expression in node.operands) {
                    val isSupervisorJobExpr =
                        context.inheritsFrom(COROUTINE_SUPERVISOR_JOB, expression)

                    if (isSupervisorJobExpr) {
                        return createSupervisorJobFix(context, expression)
                    }
                }

                return null
            }
            context.inheritsFrom(COROUTINE_NON_CANCELABLE, node) -> {
                return createNonCancelableJobFix(context, parentNode)
            }
            else -> null
        }
    }

    private fun createSupervisorJobFix(
        context: JavaContext,
        node: UExpression
    ): LintFix? = context.hasDependency(ANDROIDX_VIEWMODEL_KTX)?.let {
        fix()
            .replace()
            .range(context.getLocation(node))
            .all()
            .with(EMPTY)
            .build()
    }

    private fun createNonCancelableJobFix(
        context: JavaContext,
        node: UExpression
    ): LintFix? {
        return if (node.isInsideCoroutine(getApplicableMethodNames())) {
            fix()
                .replace()
                .range(context.getLocation(node))
                .text(METHOD_LAUNCH)
                .with(COROUTINE_WITH_CONTEXT)
                .build()
        } else null
    }
}