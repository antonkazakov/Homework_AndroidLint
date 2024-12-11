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
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.kotlin.KotlinUBinaryExpression
import org.jetbrains.uast.kotlin.toPsiType


@Suppress("UnstableApiUsage")
class JobInBuilderUsageDetector : Detector(), Detector.UastScanner {

    override fun getApplicableMethodNames(): List<String> {
        return listOf("async", "launch")
    }

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val receiverType = node.receiverType ?: return
        if (!isSubtypeOf(context, receiverType, COROUTINE_SCOPE_CLASS_FULL)) return

        for (argument in node.valueArguments) {
            if (isValidArgument(context, argument)) {
                context.report(
                    issue = ISSUE,
                    scope = node,
                    location = context.getLocation(argument),
                    message = DESCRIPTION,
                    quickfixData = createFix(context, node, argument, getEnclosingClass(node))
                )
            }
        }
    }

    private fun getEnclosingClass(node: UElement): PsiType? {
        val psiElement = node.sourcePsi
        return psiElement?.getParentOfType<KtClass>(true)?.toPsiType()
    }

    private fun isValidArgument(
        context: JavaContext,
        argument: UExpression
    ) = isSubtypeOf(context, argument.getExpressionType(), JOB_CLASS_FULL) ||
            isSubtypeOf(context, argument.getExpressionType(), COROUTINE_CONTEXT_CLASS_FULL)

    private fun createFix(
        context: JavaContext,
        callExprNode: UCallExpression,
        arg: UExpression,
        enclosingClass: PsiType?
    ): LintFix? {
        return when {
            isDependencyPresent(context, DEPENDENCY_LIFECYCLE_VIEW_MODEL_KTX) &&
            isInViewModelClassAndOnViewmodelScope(context, enclosingClass, callExprNode) &&
                    arg.isSuperVisorJobConstruction() ->
                removeJob(context, arg)

            isInViewModelClassAndOnViewmodelScope(context, enclosingClass, callExprNode) &&
                    (arg is KotlinUBinaryExpression &&
                            arg.operands.any { it.isSuperVisorJobConstruction() }) ->
                getFirstNonSuperVisorJobOperandOrNull(arg.operands)?.let { operand ->
                    replaceWithOperand(context, arg, operand)
                }

            arg.sourcePsi?.text == NON_CANCELLABLE_OBJECT ->
                replaceCallExpressionWithContext(context, callExprNode)

            else -> null
        }
    }

    private fun isInViewModelClassAndOnViewmodelScope(
        context: JavaContext,
        enclosingClass: PsiType?,
        callExprNode: UCallExpression
    ) = isSubtypeOf(context, enclosingClass, VIEW_MODEL_CLASS_FULL) &&
            callExprNode.receiver?.sourcePsi?.text == VIEW_MODEL_SCOPE

    private fun getFirstNonSuperVisorJobOperandOrNull(operands: List<UExpression>): String? =
        (operands.first { op ->
            op.sourcePsi?.text != SUPERVISOR_JOB_CONSTRUCTION_CALL
        }).sourcePsi?.text

    private fun UExpression.isSuperVisorJobConstruction() =
        this.sourcePsi?.text == SUPERVISOR_JOB_CONSTRUCTION_CALL

    private fun removeJob(
        context: JavaContext,
        node: UExpression
    ) = fix().replace()
        .range(context.getLocation(node))
        .all()
        .with("")
        .build()

    private fun replaceWithOperand(
        context: JavaContext,
        node: UExpression,
        replaceWithText: String
    ) = fix()
        .replace()
        .range(context.getLocation(node))
        .all()
        .with(replaceWithText)
        .build()

    private fun replaceCallExpressionWithContext(
        context: JavaContext,
        node: UCallExpression
    ) = fix().replace()
        .range(context.getLocation(node))
        .text(node.methodName)
        .with(WITH_CONTEXT)
        .build()


    companion object {

        private const val DESCRIPTION = "Don't use Job in builders"
        private const val EXPLANATION = "Использование Job/SupervisorJob() внутри корутин-билдеров не имеет " +
                "никакого эффекта, это может сломать ожидаемые обработку ошибок и механизм отмены корутин."
        private const val SUPERVISOR_JOB_CONSTRUCTION_CALL = "SupervisorJob()"
        private const val NON_CANCELLABLE_OBJECT = "NonCancellable"
        private const val VIEW_MODEL_SCOPE = "viewModelScope"
        private const val WITH_CONTEXT = "withContext"
        private const val VIEW_MODEL_CLASS_FULL = "androidx.lifecycle.ViewModel"
        private const val JOB_CLASS_FULL = "kotlinx.coroutines.Job"
        private const val COROUTINE_CONTEXT_CLASS_FULL = "kotlin.coroutines.CoroutineContext"
        private const val COROUTINE_SCOPE_CLASS_FULL = "kotlinx.coroutines.CoroutineScope"
        const val DEPENDENCY_LIFECYCLE_VIEW_MODEL_KTX = "androidx.lifecycle:lifecycle-viewmodel-ktx"

        val ISSUE = Issue.create(
            id = "JobInBuilderUsage",
            briefDescription = DESCRIPTION,
            explanation = EXPLANATION,
            category = Category.CORRECTNESS,
            priority = 6,
            severity = Severity.WARNING,
            implementation = Implementation(
                JobInBuilderUsageDetector::class.java, Scope.JAVA_FILE_SCOPE
            )
        )
    }
}

