@file:Suppress("UnstableApiUsage")

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
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.getParentOfType

class CoroutineJobDetector : Detector(), Detector.UastScanner {

    override fun getApplicableMethodNames(): List<String> {
        return listOf("launch", "async")
    }

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        super.visitMethodCall(context, node, method)

        if (node.receiverType?.canonicalText != "kotlinx.coroutines.CoroutineScope") return

        var jobArg: JobArg? = null
        for (arg in node.valueArguments) {
            jobArg = checkArg(arg)
            if (jobArg != null) {
                break
            }
        }

        if (jobArg == null) return

        val isViewModel = context.evaluator.extendsClass(
            node.getParentOfType<UClass>(),
            "androidx.lifecycle.ViewModel"
        )
        val isViewModelScope =
            (node.receiver as? USimpleNameReferenceExpression)?.identifier == "viewModelScope"

        if (isViewModel && isViewModelScope) {
            if (jobArg.name == SUPERVISOR_JOB) {
                context.report(
                    issue = ISSUE,
                    scope = node,
                    location = context.getLocation(node),
                    message = DESCRIPTION,
                    quickfixData = LintFix.create()
                        .replace()
                        .text(if (jobArg.isBinary) "${jobArg.name}() + " else "${jobArg.name}()")
                        .with("")
                        .build()
                )
            } else if (jobArg.name == NON_CANCELABLE_JOB) {
                context.report(
                    issue = ISSUE,
                    scope = node,
                    location = context.getLocation(node),
                    message = DESCRIPTION,
                    quickfixData = createNonCancelableFix(context, node, jobArg)
                )
            }
        } else {
            context.report(
                issue = ISSUE,
                scope = node,
                location = context.getLocation(node),
                message = DESCRIPTION,
                quickfixData = null
            )
        }
    }

    private fun checkArg(arg: UExpression, isBinary: Boolean = false): JobArg? {
        return if (arg is UCallExpression) {
            val name = arg.methodName
            if (name == SUPERVISOR_JOB || name == JOB) {
                JobArg(name = name, isBinary = isBinary)
            } else {
                null
            }
        } else if (arg is UReferenceExpression && arg.asSourceString() == NON_CANCELABLE_JOB) {
            JobArg(name = NON_CANCELABLE_JOB, isBinary = isBinary)
        } else if (arg is UBinaryExpression) {
            checkArg(arg.leftOperand, true) ?: checkArg(arg.rightOperand, true)
        } else {
            null
        }
    }

    private fun createNonCancelableFix(
        context: JavaContext,
        node: UCallExpression,
        jobArg: JobArg
    ): LintFix {
        val fix = LintFix.create().composite()
        val deleteJobFix = LintFix.create()
            .replace()
            .text(if (jobArg.isBinary) "${jobArg.name} + " else jobArg.name)
            .with("")
            .build()
        fix.add(deleteJobFix)

        val block =
            (node.valueArguments.find { it is ULambdaExpression } as? ULambdaExpression)?.body

        if (block != null) {
            val addWithContextFixBegin = LintFix.create()
                .replace()
                .range(context.getLocation(block))
                .beginning()
                .with("withContext($NON_CANCELABLE_JOB) {")
                .build()
            fix.add(addWithContextFixBegin)

            val addWithContextFixEnd = LintFix.create()
                .replace()
                .range(context.getLocation(block))
                .end()
                .with("}")
                .reformat(true)
                .build()
            fix.add(addWithContextFixEnd)
        }
        return fix.build()
    }

    companion object {
        private val DESCRIPTION =
            "Использование Job в коуртин билдерах"
        private val EXPLANATION =
            "Использование Job в коуртин билдерах приводит к разрушению связи между корутинами, что может привести к неожидаемому поведению в обработке ошибок и отмене."

        val ISSUE = Issue.create(
            id = "JobInBuilderUsage",
            briefDescription = DESCRIPTION,
            explanation = EXPLANATION,
            category = Category.create("TestCategory", 77),
            priority = 7,
            severity = Severity.WARNING,
            implementation = Implementation(CoroutineJobDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )

        private val SUPERVISOR_JOB = "SupervisorJob"
        private val JOB = "Job"
        private val NON_CANCELABLE_JOB = "NonCancellable"
    }

    data class JobArg(
        val name: String,
        val isBinary: Boolean = false
    )
}