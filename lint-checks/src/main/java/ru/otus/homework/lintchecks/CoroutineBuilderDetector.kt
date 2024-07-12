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
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.kotlin.KotlinUBinaryExpression

@Suppress("UnstableApiUsage")
class CoroutineBuilderDetector : Detector(), Detector.UastScanner {

    override fun getApplicableMethodNames(): List<String> {
        return listOf("launch", "async")
    }

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val receiverType = context.evaluator.getTypeClass(node.receiverType) ?: return


        if (receiverType.findClass(context, "kotlinx.coroutines.CoroutineScope") == null) return

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
        .with("withContext")
        .build()


    private fun isJobInViewModel(
        context: JavaContext,
        node: UCallExpression
    ): Boolean {
        val isSupervisorJob = isInheritsFromClass(
            context, node, "kotlinx.coroutines.CompletableJob"
        )

        val psiClass = node.getContainingUClass()?.javaPsi ?: return false

        val isViewModelContext = psiClass.findClass(context, "androidx.lifecycle.ViewModel") != null

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

        private const val DESCRIPTION = "Do not pass Job/SupervisorJob into coroutine builders"

        val ISSUE = Issue.create(
            id = "CoroutineBuilderIssue",
            briefDescription = DESCRIPTION,
            explanation = "Passing Job/SupervisorJob into coroutine builders can lead to errors.",
            category = Category.CORRECTNESS,
            priority = 10,
            severity = Severity.ERROR,
            implementation = Implementation(
                CoroutineBuilderDetector::class.java, Scope.JAVA_FILE_SCOPE
            )
        )
    }
}
