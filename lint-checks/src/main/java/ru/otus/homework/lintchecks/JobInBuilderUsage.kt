package ru.otus.homework.lintchecks

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Detector.UastScanner
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.kotlin.KotlinUBinaryExpression

@Suppress("UnstableApiUsage")
class JobInBuilderUsage : Detector(), UastScanner {

    override fun getApplicableMethodNames(): List<String> = listOf("launch", "async")


    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val receiverType = context.evaluator.getTypeClass(node.receiverType)

        if (!context.evaluator.inheritsFrom(
                receiverType, "kotlinx.coroutines.CoroutineScope", false
            )
        ) return

        node.valueArguments.forEach { valArg ->
            val param = context.evaluator.getTypeClass(valArg.getExpressionType())

            val isContainJob = context.evaluator.inheritsFrom(
                param, "kotlinx.coroutines.Job", false
            ) || (valArg is KotlinUBinaryExpression && context.evaluator.inheritsFrom(
                param, "kotlin.coroutines.CoroutineContext", false
            ))

            if (isContainJob) {
                context.report(
                    issue = ISSUE,
                    scope = node,
                    location = context.getLocation(valArg),
                    message = "It is not allowed to use Job or SupervisorJob in a coroutine constructor",
                    quickfixData = createFix(context, valArg, node)
                )
            }
        }
    }

    private fun createFix(
        context: JavaContext,
        node: UExpression,
        parentNode: UExpression,
    ): LintFix? {
        val param = context.evaluator.getTypeClass(node.getExpressionType())
        val isSupervisorJob = context.evaluator.inheritsFrom(
            param, "kotlinx.coroutines.SupervisorJob", false
        )

        if (checkParentClass(
                context, node.getContainingUClass(), "ViewModel", "androidx.lifecycle"
            ) && isSupervisorJob
        ) {
            return createSupervisorJobFix(context, node)
        }

        val isCoroutineContextWithOperator =
            node is KotlinUBinaryExpression && context.evaluator.inheritsFrom(
                param, "kotlin.coroutines.CoroutineContext", false
            )

        if (isCoroutineContextWithOperator) {
            (node as KotlinUBinaryExpression).operands.forEach { expression ->
                if (context.evaluator.inheritsFrom(
                        context.evaluator.getTypeClass(expression.getExpressionType()),
                        "kotlinx.coroutines.SupervisorJob",
                        false
                    )
                ) {
                    return fix().replace().range(context.getLocation(node)).with("").build()
                }
            }
        }

        if (context.evaluator.inheritsFrom(
                param, "kotlinx.coroutines.NonCancellable", false
            )
        ) return createNonCancelableFix(context, parentNode)

        return null
    }

    private fun checkParentClass(
        context: JavaContext,
        uClass: UClass?,
        className: String,
        packageName: String,
    ): Boolean {
        var psiClass = uClass?.javaPsi
        while (!(psiClass == null || psiClass.name == className)) psiClass = psiClass.superClass
        return psiClass != null && context.evaluator.getPackage(psiClass)?.qualifiedName == packageName
    }

    private fun createSupervisorJobFix(
        context: JavaContext,
        node: UExpression,
    ): LintFix {
        return fix().replace().range(context.getLocation(node)).with("").build()
    }

    private fun createNonCancelableFix(
        context: JavaContext,
        node: UExpression,
    ): LintFix? {
        if (isInAnotherCoroutine(node)) {
            return fix().replace().range(context.getLocation(node)).text("launch")
                .with("withContext").build()
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
        @JvmField
        val ISSUE: Issue = Issue.create(
            id = "JobInBuilderUsage",
            briefDescription = "It is not allowed to use Job or SupervisorJob in a coroutine constructor",
            explanation = """
          Passing **Job/SupervisorJob** to coroutine constructors results in errors.
          """,
            category = Category.CORRECTNESS,
            priority = 6,
            severity = Severity.WARNING,
            implementation = Implementation(JobInBuilderUsage::class.java, Scope.JAVA_FILE_SCOPE),
        )
    }
}