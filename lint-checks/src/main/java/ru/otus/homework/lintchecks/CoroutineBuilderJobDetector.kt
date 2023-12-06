package ru.otus.homework.lintchecks

import com.android.tools.lint.detector.api.*
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression

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
        if (
            context.evaluator.getTypeClass(node.receiverType)?.qualifiedName == "kotlinx.coroutines.CoroutineScope" &&
            node.valueArguments.any { it.asSourceString().contains("SupervisorJob") }
        ) {
            context.report(
                ISSUE,
                node,
                context.getLocation(node),
                BRIEF_DESCRIPTION
            )
        }
    }
}