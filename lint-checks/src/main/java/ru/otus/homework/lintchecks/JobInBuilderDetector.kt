package ru.otus.homework.lintchecks

import com.android.tools.lint.detector.api.*
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.*
import org.jetbrains.uast.visitor.AbstractUastVisitor

@Suppress("UnstableApiUsage")
class JobInBuilderDetector : Detector(), Detector.UastScanner {

    override fun getApplicableMethodNames(): List<String> {
        return listOf(ASYNC_METHOD, LAUNCH_METHOD)
    }

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {

        val receiverPsiClass = context.evaluator.getTypeClass(node.receiverType)
        val isPerformOnCoroutineScope =
            context.evaluator.inheritsFrom(receiverPsiClass, COROUTINE_SCOPE, false)
        if (!isPerformOnCoroutineScope) return

        val isViewModelChild =
            node.getContainingUClass()?.javaPsi?.hasParent(context, VIEW_MODEL) == true
        val parentMethodLocation = node.methodIdentifier
        val parentScopeLocation = node.receiver
        val isViewModelScope =
            (parentScopeLocation as? USimpleNameReferenceExpression)?.identifier == VIEW_MODEL_SCOPE
        val isViewModelArtifactPlugged = hasArtifact(context, LIFECYCLE_VIEWMODEL_ARTEFACT)

        node.valueArguments.forEach { argument ->
            if (argument is UBinaryExpression) {
                argument.operands.forEach { operand ->
                    checkUElement(context,
                        operand,
                        isViewModelArtifactPlugged,
                        parentMethodLocation,
                        parentScopeLocation,
                        isViewModelScope,
                        isViewModelChild)
                }
            }
            checkUElement(context,
                argument,
                isViewModelArtifactPlugged,
                parentMethodLocation,
                parentScopeLocation,
                isViewModelScope,
                isViewModelChild)
        }
    }

    private fun checkUElement(
        context: JavaContext,
        argument: UExpression,
        isViewModelArtifactPlugged: Boolean,
        parentMethodLocation: UIdentifier?,
        parentScopeLocation: UExpression?,
        isViewModelScope: Boolean,
        isViewModelChild: Boolean,
    ) {
        val argumentPsiClass = context.evaluator.getTypeClass(argument.getExpressionType())
        val isContainsJob =
            context.evaluator.inheritsFrom(argumentPsiClass, JOB, false)
        if (!isContainsJob) return

        if (isViewModelArtifactPlugged) {
            val isContainsNonCancellable =
                context.evaluator.inheritsFrom(argumentPsiClass, NON_CANCELABLE, false)
            if (isContainsNonCancellable) {
                if (parentMethodLocation != null && parentScopeLocation != null) {
                    lintReport(context,
                        argument,
                        argument,
                        nonCancellableFix(context, parentScopeLocation, parentMethodLocation))
                }
                return
            }

            var isSupervisorJobFixed = false
            argument.accept(
                object : AbstractUastVisitor() {
                    override fun visitElement(node: UElement): Boolean {
                        if (node is UIdentifier) {
                            val isContainSupervisorJob = node.name == SUPERVISOR_JOB
                            if (isContainSupervisorJob && isViewModelScope && isViewModelChild) {
                                val uCallExpression = node.uastParent as? UCallExpression
                                val elementForReplace = uCallExpression ?: node
                                lintReport(context,
                                    argument,
                                    argument,
                                    supervisorJobFix(context, elementForReplace))
                                isSupervisorJobFixed = true
                            }
                        }
                        return super.visitElement(node)
                    }
                }
            )
            if (isSupervisorJobFixed) return
        } else {
            lintReport(context, argument, argument, null)
        }
    }

    private fun supervisorJobFix(context: JavaContext, node: UElement): LintFix =
        fix()
            .replace()
            .range(context.getLocation(node))
            .all()
            .with(EMPTY_STRING)
            .build()

    private fun nonCancellableFix(
        context: JavaContext,
        start: UElement,
        end: UElement,
    ): LintFix =
        fix()
            .replace()
            .range(context.getRangeLocation(start, 0, end, 0))
            .with(COROUTINE_WITH_CONTEXT)
            .all()
            .build()

    private fun lintReport(
        context: JavaContext,
        node: UElement,
        argument: UElement,
        fix: LintFix?,
    ) {
        context.report(
            issue = ISSUE,
            scope = node,
            location = context.getLocation(argument),
            message = BRIEF_DESCRIPTION,
            quickfixData = fix
        )
    }

    companion object {

        private const val ID = "JobInBuilderUsage"
        private const val BRIEF_DESCRIPTION =
            "Job or SupervisorJob use in coroutine builder is not allowed."
        private const val EXPLANATION =
            "Job or SupervisorJob in coroutine builder has no benefits but has a negative impact to coroutine exceptions handling and coroutine cancellation."
        private const val PRIORITY = 1
        private const val SUPERVISOR_JOB = "SupervisorJob"
        private const val JOB = "kotlinx.coroutines.Job"
        private const val ASYNC_METHOD = "async"
        private const val LAUNCH_METHOD = "launch"
        private const val LIFECYCLE_VIEWMODEL_ARTEFACT =
            "androidx.lifecycle:lifecycle-viewmodel-ktx"
        private const val VIEW_MODEL = "androidx.lifecycle.ViewModel"
        private const val COROUTINE_SCOPE = "kotlinx.coroutines.CoroutineScope"
        private const val VIEW_MODEL_SCOPE = "viewModelScope"
        private const val NON_CANCELABLE = "kotlinx.coroutines.NonCancellable"
        private const val COROUTINE_WITH_CONTEXT = "withContext"
        private const val EMPTY_STRING = ""

        val ISSUE = Issue.create(
            id = ID,
            briefDescription = BRIEF_DESCRIPTION,
            explanation = EXPLANATION,
            category = Category.LINT,
            priority = PRIORITY,
            severity = Severity.WARNING,
            implementation = Implementation(JobInBuilderDetector::class.java,
                Scope.JAVA_FILE_SCOPE)
        )
    }
}