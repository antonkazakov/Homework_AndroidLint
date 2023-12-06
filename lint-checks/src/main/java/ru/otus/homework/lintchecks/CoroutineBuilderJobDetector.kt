package ru.otus.homework.lintchecks

import com.android.tools.lint.client.api.JavaEvaluator
import com.android.tools.lint.detector.api.*
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.*

private const val ID = "JobInBuilderUsage"
private const val BRIEF_DESCRIPTION = "Не используйте Job/SupervisorJob внутри корутин-билдеров"
private const val EXPLANATION = """Использование Job/SupervisorJob внутри корутин-билдеров не имеет никакого эффекта.
     это может сломать ожидаемые обработку ошибок и механизм отмены корутин."""

class CoroutineBuilderJobDetector : Detector(), Detector.UastScanner {

    companion object {
        val ISSUE = Issue.create(
            id = ID,
            briefDescription = BRIEF_DESCRIPTION,
            explanation = EXPLANATION,
            category = Category.CORRECTNESS,
            priority = 6,
            severity = Severity.WARNING,
            implementation = Implementation(
                CoroutineBuilderJobDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }

    override fun getApplicableMethodNames(): List<String> {
        return listOf("launch", "async")
    }

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val evaluator = context.evaluator
        if (
            evaluator.inheritsFrom(evaluator.getTypeClass(node.receiverType), "kotlinx.coroutines.CoroutineScope") &&
            checkArgument(evaluator, node.valueArguments[0])
        ) {
            context.report(
                ISSUE,
                node,
                context.getLocation(node),
                BRIEF_DESCRIPTION
            )
        }
    }

    private fun checkArgument(evaluator: JavaEvaluator, node: UExpression): Boolean {
        return when (node) {
            is UBinaryExpression -> {
                checkArgument(evaluator, node.leftOperand) || checkArgument(evaluator, node.rightOperand)
            }
            is UCallExpression -> {
                node.asSourceString() in setOf("SupervisorJob()", "Job()")
            }
            is UParenthesizedExpression -> {
                checkArgument(evaluator, node.expression)
            }
            else -> false
        }
    }
}