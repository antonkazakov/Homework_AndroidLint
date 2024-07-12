package ru.otus.homework.lintchecks.detectors

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UParenthesizedExpression
import org.jetbrains.uast.UReferenceExpression
import ru.otus.homework.lintchecks.LintConstants.ISSUE_CATEGORY_NAME
import ru.otus.homework.lintchecks.LintConstants.ISSUE_MAX_PRIORITY

@Suppress("UnstableApiUsage")
class JobInBuilderUsageDetector : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> {
        return listOf(UCallExpression::class.java)
    }

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return MethodCallHandler(context)
    }

    class MethodCallHandler(private val context: JavaContext) : UElementHandler() {
        override fun visitCallExpression(node: UCallExpression) {
            if (node.methodName in listOf("launch", "async")) {
                checkArgument(node.valueArguments.firstOrNull())
            }
        }

        private fun checkArgument(argument: UExpression?) {
            when (argument) {
                is UParenthesizedExpression -> checkArgument(argument.expression)
                is UBinaryExpression -> {
                    checkArgument(argument.leftOperand)
                    checkArgument(argument.rightOperand)
                }

                is UCallExpression,
                is UReferenceExpression -> {
                    if (argument.getExpressionType()?.canonicalText in
                        listOf(COMPLETABLE_JOB_CLASS, JOB_CLASS, NON_CANCELABLE_CLASS)
                    ) {
                        context.report(
                            issue = ISSUE,
                            scope = argument,
                            location = context.getLocation(argument),
                            message = BRIEF_DESCRIPTION
                        )
                    }
                }
            }

        }
    }

    companion object {
        private const val ID = "JobInBuilderUsage"
        private const val BRIEF_DESCRIPTION =
            "Найден экземпляр Job/SupervisorJob в корутин билдер"
        private const val EXPLANATION =
            "Частая ошибка при использовании корутин - передача экземпляра Job/SupervisorJob в корутин билдер. Хоть Job и его наследники являются элементами CoroutineContext, их использование внутри корутин-билдеров не имеет никакого эффекта, это может сломать ожидаемые обработку ошибок и механизм отмены корутин."

        private const val COMPLETABLE_JOB_CLASS = "kotlinx.coroutines.CompletableJob"
        private const val NON_CANCELABLE_CLASS = "kotlinx.coroutines.NonCancellable"
        private const val JOB_CLASS = "kotlinx.coroutines.Job"

        val ISSUE = Issue.create(
            id = ID,
            briefDescription = BRIEF_DESCRIPTION,
            explanation = EXPLANATION,
            category = Category.create(ISSUE_CATEGORY_NAME, ISSUE_MAX_PRIORITY),
            priority = 7,
            severity = Severity.WARNING,
            implementation = Implementation(
                JobInBuilderUsageDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )

    }
}