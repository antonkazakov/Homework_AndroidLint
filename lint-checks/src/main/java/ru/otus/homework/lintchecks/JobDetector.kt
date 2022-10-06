package ru.otus.homework.lintchecks

import com.android.tools.lint.detector.api.*
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.kotlin.KotlinUBinaryExpression

@Suppress("UnstableApiUsage")
class JobDetector : Detector(), Detector.UastScanner {

    override fun getApplicableMethodNames(): List<String> {
        return listOf(LAUNCH, ASYNC)
    }

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val receiver = context.evaluator.getTypeClass(node.receiverType)

        receiver
            ?.find(
                iterator = { it.superClass },
                predicate = { psiClass ->
                    psiClass.name == COROUTINE_SCOPE &&
                            context.evaluator.getPackage(psiClass)?.qualifiedName == KOTLINX_COROUTINE
                }
            ) ?: return

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
        val viewModelElement = node.getContainingUClass()?.javaPsi
            ?.find(
                iterator = { it.superClass },
                predicate = { psiClass ->
                    psiClass.name == "ViewModel" &&
                            context.evaluator.getPackage(psiClass)?.qualifiedName == "androidx.lifecycle"
                }
            )

        val param = context.evaluator.getTypeClass(node.getExpressionType())
        val isSupervisorJob = context.evaluator
            .inheritsFrom(param, SUPERVISOR_JOB, false)

        if (viewModelElement != null && isSupervisorJob) {
            return createSupervisorJobFix(context, node)
        }

        val isCoroutineContextWithOperator = node is KotlinUBinaryExpression &&
                context.evaluator.inheritsFrom(
                    param,
                    COROUTINE_CONTEXT,
                    false
                )

        if (isCoroutineContextWithOperator) {
            (node as KotlinUBinaryExpression).operands.forEach { expression ->
                val isSupervisorJobExpr = context.evaluator.inheritsFrom(
                    context.evaluator.getTypeClass(expression.getExpressionType()),
                    SUPERVISOR_JOB,
                    false
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
        val containViewModelArtifact = context.evaluator.dependencies?.getAll()?.any {
            it.identifier.contains("androidx.lifecycle:lifecycle-viewmodel-kt")
        }

        if (containViewModelArtifact != true) return null

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
        private const val BRIEF = "brief description"
        private const val EXPLANATION = "explanation"
        private const val ID = "JobInBuilderUsage"
        private const val LAUNCH = "launch"
        private const val ASYNC = "async"
        private const val KOTLINX_COROUTINE = "kotlinx.coroutines"
        private const val COROUTINE_SCOPE = "CoroutineScope"
        private const val NON_CANCELABLE = "kotlinx.coroutines.NonCancellable"
        private const val SUPERVISOR_JOB = "kotlinx.coroutines.CompletableJob"
        private const val COROUTINE_CONTEXT = "kotlin.coroutines.CoroutineContext"
        private const val JOB = "kotlinx.coroutines.Job"

        val ISSUE = Issue.create(
            id = ID,
            briefDescription = BRIEF,
            explanation = EXPLANATION,
            category = Category.LINT,
            priority = 3,
            severity = Severity.ERROR,
            implementation = Implementation(JobDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )

    }

}