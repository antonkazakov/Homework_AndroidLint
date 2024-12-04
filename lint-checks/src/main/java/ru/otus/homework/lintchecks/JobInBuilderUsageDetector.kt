package ru.otus.homework.lintchecks

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.kotlin.KotlinUBinaryExpression

fun PsiClass.findClass(
    context: JavaContext,
    classPath: String
): PsiClass? {
    var currentClass: PsiClass? = this
    val (classPackage, className) = classPath.substringBeforeLast('.') to classPath.substringAfterLast('.')

    while (currentClass != null) {
        val isMatchingClassName = currentClass.name == className
        val isMatchingClassPackage =
            context.evaluator.getPackage(currentClass)?.qualifiedName == classPackage

        if (isMatchingClassName && isMatchingClassPackage) {
            return currentClass
        }

        currentClass = currentClass.superClass
    }

    return null
}

@Suppress("UnstableApiUsage")
class JobInBuilderUsageDetector : Detector(), Detector.UastScanner {

    override fun getApplicableMethodNames(): List<String> {
        return listOf("async", "launch")
    }

    private fun isSubclassOf(
        context: JavaContext, psiType: PsiType?, superClassName: String
    ): Boolean {
        return psiType?.let { type ->
            context.evaluator.getTypeClass(type)?.extendsClass(context, superClassName) == true
        } ?: false
    }

    private fun PsiClass.extendsClass(context: JavaContext, className: String): Boolean =
        context.evaluator.extendsClass(this, className, false)

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val receiverType = node.receiverType ?: return
        if (!isSubclassOf(context, receiverType, "kotlinx.coroutines.CoroutineScope")) return

        for (argument in node.valueArguments) {
            if (isValidArgument(context, argument)) {
                context.report(
                    issue = ISSUE,
                    scope = node,
                    location = context.getLocation(argument),
                    message = DESCRIPTION,
                    quickfixData = getQuickFix(context, node)
                )
            }
        }
    }

    private fun isValidArgument(
        context: JavaContext,
        argument: UExpression
    ) = isSubclassOf(context, argument.getExpressionType(), "kotlinx.coroutines.Job") ||
            isSubclassOf(context, argument.getExpressionType(), "kotlin.coroutines.CoroutineContext")

    private fun getQuickFix(
        context: JavaContext,
        node: UCallExpression
    ) = when {
        isJobInViewModel(context, node) -> suggestRemoveJob(context, node)
        isNonCancellableJob(context, node) -> suggestReplaceCall(context, node)
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

        private const val DESCRIPTION = "Don't use Job in builders"

        val ISSUE = Issue.create(
            id = "JobInBuilderUsage",
            briefDescription = DESCRIPTION,
            explanation = "Passing Job/SupervisorJob into coroutine builders can lead to errors.",
            category = Category.CORRECTNESS,
            priority = 10,
            severity = Severity.WARNING,
            implementation = Implementation(
                JobInBuilderUsageDetector::class.java, Scope.JAVA_FILE_SCOPE
            )
        )
    }
}

