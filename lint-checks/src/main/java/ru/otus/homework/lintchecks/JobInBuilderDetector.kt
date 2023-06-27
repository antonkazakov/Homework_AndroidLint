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
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.kotlin.KotlinUBinaryExpression

@Suppress("UnstableApiUsage")
class JobInBuilderDetector : Detector(), Detector.UastScanner {

    override fun getApplicableMethodNames(): List<String> {
        return listOf(LAUNCH, ASYNC)
    }

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val receiver = context.evaluator.getTypeClass(node.receiverType)

        val isPerformOnCoroutineScope =
            context.evaluator.inheritsFrom(receiver, COROUTINE_SCOPE, false)
        if (!isPerformOnCoroutineScope) return

        node.valueArguments.forEach { arg ->
            val param = context.evaluator.getTypeClass(arg.getExpressionType())
            val isContainJob = context.evaluator.inheritsFrom(param, JOB, false) ||
                    (arg is KotlinUBinaryExpression &&
                            context.evaluator.inheritsFrom(param, COROUTINE_CONTEXT, false))

            if (isContainJob) {
                context.report(
                    issue = ISSUE,
                    scope = node,
                    location = context.getLocation(arg),
                    message = BRIEF,
                    quickfixData = createFix(context, arg, node)
                )
            }
        }
    }

    private fun createFix(
        context: JavaContext,
        node: UExpression,
        parentNode: UExpression
    ): LintFix? {
        val psiClass = node.getContainingUClass()?.javaPsi

        val param = context.evaluator.getTypeClass(node.getExpressionType())
        val isSupervisorJob = context.evaluator.inheritsFrom(
            cls = param,
            className = SUPERVISOR_JOB,
            strict = false
        )

        if (psiClass?.hasParent(context, "androidx.lifecycle.ViewModel") == true &&
            isSupervisorJob
        ) {
            return createSupervisorJobFix(context, node)
        }

        val isCoroutineContextWithOperator = node is KotlinUBinaryExpression &&
                context.evaluator.inheritsFrom(
                    cls = param,
                    className = COROUTINE_CONTEXT,
                    strict = false
                )

        if (isCoroutineContextWithOperator) {
            (node as KotlinUBinaryExpression).operands.forEach { expression ->
                val isSupervisorJobExpr = context.evaluator.inheritsFrom(
                    cls = context.evaluator.getTypeClass(expression.getExpressionType()),
                    className = SUPERVISOR_JOB,
                    strict = false
                )
                if (isSupervisorJobExpr) {
                    return createSupervisorJobFix(context, expression)
                }
            }
        }

        val isNonCancelableJob = context.evaluator
            .inheritsFrom(param, NON_CANCELABLE, false)

        if (isNonCancelableJob) {
            return createNonCancelableJobFix(context, parentNode)
        }

        return null
    }

    private fun createSupervisorJobFix(
        context: JavaContext,
        node: UExpression
    ): LintFix? {
        val hasViewModelArtifact = hasArtifact(
            context = context,
            artifactName = "androidx.lifecycle:lifecycle-viewmodel-ktx"
        )

        if (!hasViewModelArtifact) return null

        return fix()
            .replace()
            .range(context.getLocation(node))
            .all()
            .with("")
            .build()
    }

    private fun createNonCancelableJobFix(
        context: JavaContext,
        node: UExpression
    ): LintFix? {
        if (isInAnotherCoroutine(node)) {
            return fix()
                .replace()
                .range(context.getLocation(node))
                .text(LAUNCH)
                .with("withContext")
                .build()
        }

        return null
    }

    private fun isInAnotherCoroutine(node: UElement?): Boolean {
        return when (val parent = node?.uastParent) {
            is UCallExpression -> getApplicableMethodNames().any { methodName ->
                methodName == parent.methodName
            }

            null -> false
            else -> {
                return isInAnotherCoroutine(parent)
            }
        }
    }

    companion object {
        private const val ID = "JobInBuilderUsage"
        private const val BRIEF = "Job or SupervisorJob use in coroutine builder is not allowed."
        private const val EXPLANATION =
            "Частая ошибка при использовании корутин - передача экземпляра `Job`/`SupervisorJob` в корутин билдер. Хоть `Job` и его наследники являются элементами `CoroutineContext`, их использование внутри корутин-билдеров не имеет никакого эффекта, это может сломать ожидаемые обработку ошибок и механизм отмены корутин. Использование еще одного наследника `Job` - `NonCancellable` внутри корутин-билдеров сломает обработку ошибок у всех корутин в иерархии"
        private const val PRIORITY = 6
        private const val LAUNCH = "launch"
        private const val ASYNC = "async"
        private const val COROUTINE_SCOPE = "kotlinx.coroutines.CoroutineScope"
        private const val NON_CANCELABLE = "kotlinx.coroutines.NonCancellable"
        private const val SUPERVISOR_JOB = "kotlinx.coroutines.SupervisorJob"
        private const val COROUTINE_CONTEXT = "kotlin.coroutines.CoroutineContext"
        private const val JOB = "kotlinx.coroutines.Job"

        val ISSUE = Issue.create(
            id = ID,
            briefDescription = BRIEF,
            explanation = EXPLANATION,
            category = Category.LINT,
            priority = PRIORITY,
            severity = Severity.WARNING,
            implementation = Implementation(JobInBuilderDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }
}
