package ru.otus.homework.lintchecks

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UParenthesizedExpression
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.getParentOfType

@Suppress("UnstableApiUsage")
class JobInBuilderUsageDetector : Detector(), Detector.UastScanner {

    companion object {
        val ISSUE = Issue.create(
            id = ID,
            briefDescription = BRIEF_DESCRIPTION,
            explanation = EXPLANATION,
            category = Category.CORRECTNESS,
            priority = PRIORITY,
            severity = Severity.WARNING,
            implementation = Implementation(
                JobInBuilderUsageDetector::class.java, Scope.JAVA_FILE_SCOPE
            )
        )
    }
    override fun getApplicableUastTypes(): List<Class<out UElement>> {
        return listOf(UCallExpression::class.java)
    }

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return MethodCallHandler(context)
    }

    class MethodCallHandler(private val context: JavaContext) : UElementHandler() {
        override fun visitCallExpression(node: UCallExpression) {
            if (node.methodIdentifier?.name in listOf("launch", "async")) {
                val argument = node.valueArguments.getOrNull(0)
                checkArgument(argument, node)
            }
        }

        private fun checkArgument(
            argument: UExpression?,
            node: UCallExpression,
            isRightOperand: Boolean = false,
            isLeftOperand: Boolean = false,
            isParenthesized: Boolean = false
        ) {
            when(argument) {
                is UParenthesizedExpression -> {
                    checkArgument(
                        argument.expression,
                        node,
                        isRightOperand = isRightOperand,
                        isLeftOperand = isLeftOperand,
                        isParenthesized = true
                    )
                }
                is UBinaryExpression -> {
                    checkArgument(argument.leftOperand, node, isLeftOperand = true)
                    checkArgument(argument.rightOperand, node, isRightOperand = true)
                }
                is UCallExpression -> {
                    if (argument.getExpressionType()?.canonicalText == COROUTINES_JOB || argument.getExpressionType()?.superTypes?.any { it.canonicalText == COROUTINES_JOB } == true) {
                        val lintFix = if (
                            argument.classReference?.getExpressionType()?.canonicalText == COROUTINES_COMPLETABLE_JOB
                            && argument.methodName == SUPERVISOR_JOB_CALL_NAME
                            && isOnViewModelScope(node)
                        ) {
                            createSupervisorJobFix(isRightOperand = isRightOperand, isLeftOperand = isLeftOperand, isParenthesized = isParenthesized)
                        } else null

                        makeReport(context.getLocation(node.valueArguments[0]), lintFix)
                    }
                }
                is UReferenceExpression -> {
                    if (argument.getExpressionType()?.canonicalText == COROUTINES_JOB || argument.getExpressionType()?.superTypes?.any { it.canonicalText == COROUTINES_JOB } == true) {
                        if (
                            argument.getExpressionType()?.canonicalText == COROUTINES_NON_CANCELABLE
                            && node.receiver == null
                        ) {
                            makeReport(context.getLocation(node), createNonCancelableFix(node))
                        } else {
                            makeReport(context.getLocation(argument))
                        }
                    }
                }
            }

        }

        private fun isOnViewModelScope(node: UCallExpression): Boolean {
            return context.evaluator.extendsClass(node.getParentOfType<UClass>()?.javaPsi, LIFECYCLE_VIEW_MODEL)
                    && node.receiver is USimpleNameReferenceExpression
                    && (node.receiver as USimpleNameReferenceExpression).sourcePsi?.text == VIEW_MODEL_SCOPE
        }

        private fun createSupervisorJobFix(isRightOperand: Boolean = false, isLeftOperand: Boolean = false, isParenthesized: Boolean = false): LintFix {
            var replaceText = if (isParenthesized) {
                regexParenthesized
            } else {
                regexDef
            }

            if (isRightOperand) {
                replaceText = regexWithOperand + replaceText
            }
            if(isLeftOperand) {
                replaceText += regexWithOperand
            }

            return LintFix.create()
                .name(SUPERVISOR_JOB_FIX_NAME)
                .replace()
                .pattern(replaceText)
                .with("")
                .build()
        }

        private fun createNonCancelableFix(node: UCallExpression): LintFix {
            val coroutineBuilderName = node.methodIdentifier?.name?: ""
            return LintFix.create()
                .name("Заменить $coroutineBuilderName на withContext")
                .replace()
                .text(coroutineBuilderName)
                .with("withContext")
                .build()
        }

        private fun makeReport(location: Location, lintFix: LintFix? = null) {
            context.report(
                ISSUE,
                location,
                BRIEF_DESCRIPTION,
                lintFix
            )
        }
    }

}

private const val ID = "JobInBuilderUsage"
private const val BRIEF_DESCRIPTION = "Job/SupervisorJob нельзя передавать в корутин-билдер"
private const val MORE_LINK =
    "https://medium.com/androiddevelopers/exceptions-in-coroutines-ce8da1ec060c"

private const val EXPLANATION =
    "Использование Job внутри корутин-билдеров может сломать ожидаемые обработку ошибок и механизм отмены корутин. Подробнее: $MORE_LINK"
private const val PRIORITY = 7

private const val COROUTINES_COMPLETABLE_JOB = "kotlinx.coroutines.CompletableJob"
private const val COROUTINES_NON_CANCELABLE = "kotlinx.coroutines.NonCancellable"
private const val COROUTINES_JOB = "kotlinx.coroutines.Job"

private const val LIFECYCLE_VIEW_MODEL = "androidx.lifecycle.ViewModel"
private const val VIEW_MODEL_SCOPE = "viewModelScope"

const val regexWithOperand = """\s*\+\s*"""
const val regexParenthesized = """\(\s*SupervisorJob\s*\(\s*\)\s*\)"""
const val regexDef = """SupervisorJob\s*\(\s*\)"""

private const val SUPERVISOR_JOB_FIX_NAME = "Удалить SupervisorJob"
private const val SUPERVISOR_JOB_CALL_NAME = "SupervisorJob"