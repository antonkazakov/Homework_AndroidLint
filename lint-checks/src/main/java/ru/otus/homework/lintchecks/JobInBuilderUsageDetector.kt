package ru.otus.homework.lintchecks


import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.kotlin.KotlinUBinaryExpression

@Suppress("UnstableApiUsage")
class JobInBuilderUsageDetector : Detector(), Detector.UastScanner {
    companion object {
        private const val ISSUE_ID = "JobInBuilderUsage"
        private const val BRIEF_DESCRIPTION = "Inappropriate Job usage in coroutine builders"
        private const val EXPLANATION = """
            Passing a Job instance directly to launch or async is usually redundant or even harmful. 
            It can break expected error handling and cancellation behavior.
        """
        val ISSUE = Issue.create(
            id = ISSUE_ID,
            briefDescription = BRIEF_DESCRIPTION,
            explanation = EXPLANATION,
            category = Category.CORRECTNESS,
            priority = 6,
            severity = Severity.WARNING,
            implementation = Implementation(
                JobInBuilderUsageDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
        private const val JOB_CLASS = "kotlinx.coroutines.Job"
        private const val COROUTINE_CONTEXT_CLASS = "kotlin.coroutines.CoroutineContext"
        private const val SUPERVISOR_JOB_CLASS = "kotlinx.coroutines.SupervisorJob"
        private const val NON_CANCELLABLE_CLASS = "kotlinx.coroutines.NonCancellable"
        private const val VIEWMODEL_CLASS = "androidx.lifecycle.ViewModel"
        private val METHODS_TO_CHECK = listOf("launch", "async")
    }

    override fun getApplicableUastTypes(): List<Class<out UElement>>? {
        return listOf(UCallExpression::class.java)
    }

    override fun createUastHandler(context: JavaContext): UElementHandler? {
        return JobUsageHandler(context)
    }

    class JobUsageHandler(private val context: JavaContext) : UElementHandler() {
        override fun visitCallExpression(node: UCallExpression) {
            val methodName = (node.resolve())?.name
            if (METHODS_TO_CHECK.contains(methodName)) {
                node.valueArguments.forEach { argument ->
                    val hasPassedJob = isInheritsFromClass(
                        context = context,
                        node = argument,
                        className = JOB_CLASS
                    ) || (argument is KotlinUBinaryExpression && isInheritsFromClass(
                        context = context,
                        node = node,
                        className = COROUTINE_CONTEXT_CLASS
                    ))

                    if (!hasPassedJob) return@forEach

                    val fix = getFix(node, argument)
                    context.report(
                        issue = ISSUE,
                        scope = node,
                        location = context.getLocation(argument),
                        message = EXPLANATION,
                        quickfixData = fix
                    )
                }

            }
        }

        private fun getFix(node: UCallExpression, jobArgument: UExpression): LintFix? {
            val jobType = jobArgument.getExpressionType()?.canonicalText
            return when {
                jobType == SUPERVISOR_JOB_CLASS && isCalledOnViewModelScope(node) -> {
                    LintFix.create()
                        .replace()
                        .all()
                        .with("")
                        .range(context.getLocation(jobArgument))
                        .build()
                }

                jobType == NON_CANCELLABLE_CLASS && hasCoroutineScopeParent(node) -> {
                    LintFix.create()
                        .name("Replace with withContext")
                        .replace()
                        .text(node.methodName ?: "")
                        .with("withContext")
                        .range(context.getLocation(node.methodIdentifier ?: node))
                        .reformat(true)
                        .build()
                }

                else -> null
            }
        }

        private fun isCalledOnViewModelScope(node: UCallExpression): Boolean {
            val receiver = node.receiver as? UQualifiedReferenceExpression ?: return false
            return receiver.selector.asSourceString() == "viewModelScope"
                    && receiver.receiver is USimpleNameReferenceExpression
                    && receiver.receiver.getContainingUClass()?.javaPsi
                ?.isSubclassOf(context, VIEWMODEL_CLASS) == true
        }

        private fun hasCoroutineScopeParent(node: UCallExpression): Boolean {
            var parent = node.uastParent
            while (parent != null) {
                if (parent is UCallExpression && METHODS_TO_CHECK.contains(parent.methodName)) {
                    return true
                }
                parent = parent.uastParent
            }
            return false
        }

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
    }
}