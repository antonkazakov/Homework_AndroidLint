package ru.otus.homework.lintchecks

import com.android.tools.lint.client.api.JavaEvaluator
import com.android.tools.lint.detector.api.*
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.*

private const val ID = "JobInBuilderUsage"
private const val BRIEF_DESCRIPTION = "Не используйте Job/SupervisorJob внутри корутин-билдеров"
private const val EXPLANATION = """Использование Job/SupervisorJob внутри корутин-билдеров не имеет никакого эффекта.
     Это может сломать ожидаемые обработку ошибок и механизм отмены корутин.
     Использование NonCancellable ломает обработку ошибок у всех корутин в иерархии"""

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
                BRIEF_DESCRIPTION,
                quickFix(context, node, method)
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

            is UReferenceExpression -> {
                node.asSourceString() == "NonCancellable"
            }

            else -> false
        }
    }

    private fun quickFix(context: JavaContext, node: UCallExpression, method: PsiMethod): LintFix? {
        return when {
            useSupervisorJobInsideViewModelScope(context, node) -> {
                fix()
                    .name("На viewModelScope внутри ViewModel удалить SupervisorJob")
                    .replace()
                    .text("(SupervisorJob())")
                    .with("")
                    .autoFix()
                    .build()
            }

            useNonCancellableInsideViewModelScope(context, node) -> {
                fix()
                    .name("На viewModelScope внутри ViewModel заменить вызов launch/async на withContext")
                    .replace()
                    .text("viewModelScope.launch(NonCancellable)")
                    .with("withContext(NonCancellable)")
                    .autoFix()
                    .build()
            }

            else -> null
        }
    }

    private fun useSupervisorJobInsideViewModelScope(context: JavaContext, node: UCallExpression): Boolean {
        return isInsideViewModelScope(context, node) && node.sourcePsi?.children?.get(1)?.text == "(SupervisorJob())"
    }

    private fun useNonCancellableInsideViewModelScope(context: JavaContext, node: UCallExpression): Boolean {
        return isInsideViewModelScope(context, node) && node.sourcePsi?.children?.get(1)?.text == "(NonCancellable)"
    }

    private fun isInsideViewModelScope(context: JavaContext, node: UCallExpression): Boolean {
        return context.evaluator.extendsClass(node.getParentOfType<UClass>(), "androidx.lifecycle.ViewModel") &&
                node.receiverType?.canonicalText == "kotlinx.coroutines.CoroutineScope" &&
                node.receiver?.asSourceString() == "viewModelScope"
    }
}