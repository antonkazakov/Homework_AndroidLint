package ru.otus.homework.lintchecks

import com.android.tools.lint.detector.api.*
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.*
import org.jetbrains.uast.kotlin.KotlinUBinaryExpression

private const val ID = "JobInBuilderUsage"
private const val BRIEF_DESCRIPTION = "описание job"
private const val EXPLANATION = "Полное описание проблемы job"
private const val PRIORITY = 5

private const val COROUTINE_SCOPE = "kotlinx.coroutines.CoroutineScope"
private const val VIEWMODEL = "androidx.lifecycle.ViewModel"
private const val VIEWMODEL_ARTIFACT = "androidx.lifecycle:lifecycle-viewmodel-ktx"
private const val JOB = "kotlinx.coroutines.Job"
private const val COROUTINE_CONTEXT = "kotlin.coroutines.CoroutineContext"
private const val COROUTINE_SUPERVISOR_JOB = "kotlinx.coroutines.CompletableJob"
private const val NON_CANCELABLE = "kotlinx.coroutines.NonCancellable"

@Suppress("UnstableApiUsage")
class JobDetector : Detector(), Detector.UastScanner {

    companion object {
        val ISSUE = Issue.create(
            ID,
            BRIEF_DESCRIPTION,
            EXPLANATION,
            Category.create("HOMEWORK CATEGORY", 10),
            PRIORITY,
            Severity.WARNING,
            Implementation(JobDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }

    override fun getApplicableMethodNames() = listOf("launch", "async")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val evaluator = context.evaluator
        val clazz = evaluator.getTypeClass(node.receiverType)
        if (clazz?.hasClass(COROUTINE_SCOPE) == false) return

        node.valueArguments.forEach { argument ->
            val param = evaluator.getTypeClass(argument.getExpressionType())
            val hasJob = evaluator.inheritsFrom(param, JOB, false)
                    || (argument is KotlinUBinaryExpression && evaluator.inheritsFrom(
                param,
                COROUTINE_CONTEXT,
                false
            ))
            if (!hasJob) return@forEach
            context.reportAndFix(node = argument, parentNode =  node)
        }
    }

    private fun createFix(
        context: JavaContext,
        node: UExpression,
        parentNode: UExpression
    ): LintFix? {

        val evaluator = context.evaluator
        val clazz = node.getContainingUClass()?.javaPsi
        val param = evaluator.getTypeClass(node.getExpressionType())
        val isSupervisorJob = evaluator.inheritsFrom(param, JOB, false)

        val isNonCancelableJob = evaluator.inheritsFrom(param, NON_CANCELABLE, false)
        if (isNonCancelableJob && this.isInCoroutine(parentNode)) {
            return fix()
                .replace()
                .range(context.getLocation(parentNode))
                .text((parentNode as UCallExpression).methodName)
                .with("withContext")
                .build()
        }
        if (clazz?.hasSuperClass(VIEWMODEL) == true && context.hasDependencies(VIEWMODEL_ARTIFACT) && isSupervisorJob) {
            return fix().replace().range(context.getLocation(node)).all().with("").build()
        }

        if (node is KotlinUBinaryExpression && evaluator.inheritsFrom(
                param,
                COROUTINE_CONTEXT,
                false
            )
        ) {
            node.operands.forEach {
                val type = evaluator.getTypeClass(it.getExpressionType())
                val isSuperVisorJobExpr = evaluator.inheritsFrom(type,
                    COROUTINE_SUPERVISOR_JOB,
                    false
                )
                if (!isSuperVisorJobExpr) return@forEach
                return fix().replace().range(context.getLocation(node)).all().with("").build()
            }
        }

        return null
    }

    private fun JavaContext.reportAndFix(node: UExpression, parentNode: UExpression) {
        report(
            issue = GlobalScopeDetector.ISSUE,
            message = BRIEF_DESCRIPTION,
            scope = node,
            location = getLocation(node),
            quickfixData = createFix(this, node, parentNode)
        )
    }

    private fun isInCoroutine(
        uElement: UElement?
    ): Boolean {
        return when {
            uElement == null || uElement is UMethod -> false
            uElement is UCallExpression
                    && getApplicableMethodNames().any { it == uElement.methodName } -> true
            else -> this.isInCoroutine(
                uElement.uastParent
            )
        }
    }

}