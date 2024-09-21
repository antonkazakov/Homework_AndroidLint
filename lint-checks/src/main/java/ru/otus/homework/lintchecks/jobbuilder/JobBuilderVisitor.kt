package ru.otus.homework.lintchecks.jobbuilder

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.JavaContext
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UastCallKind
import org.jetbrains.uast.kotlin.KotlinUBinaryExpression
import ru.otus.homework.lintchecks.jobbuilder.JobBuilderIssue.BRIEF_DESCRIPTION

@Suppress("UnstableApiUsage")
class JobBuilderVisitor(private val context: JavaContext) : UElementHandler() {

    override fun visitCallExpression(node: UCallExpression) {
        if (node.kind == UastCallKind.METHOD_CALL) {
            if (node.methodIdentifier?.name in listOf("launch", "async")) {
                val receiver = context.evaluator.getTypeClass(node.receiverType)

                val isPerformOnCoroutineScope = context.evaluator.inheritsFrom(receiver, COROUTINE_SCOPE, false)
                if (!isPerformOnCoroutineScope) return

                node.valueArguments.forEach { expression ->
                    val param = context.evaluator.getTypeClass(expression.getExpressionType())
                    println(param)
                    val isExtendsJob = context.evaluator.inheritsFrom(param, JOB, false) ||
                        (expression is KotlinUBinaryExpression && context.evaluator.inheritsFrom(param, COROUTINE_CONTEXT, false))

                    if (isExtendsJob) reportIssue(node)
                }
            }
        }
    }
    private fun reportIssue(node: UCallExpression) {
        context.report(
            issue = JobBuilderIssue.ISSUE,
            scope = node,
            location = context.getNameLocation(node),
            message = BRIEF_DESCRIPTION
        )
    }

    companion object {
        private const val COROUTINE_SCOPE = "kotlinx.coroutines.CoroutineScope"
        private const val JOB = "kotlinx.coroutines.Job"
        private const val COROUTINE_CONTEXT = "kotlin.coroutines.CoroutineContext"
    }
}
