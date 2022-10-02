package ru.otus.homework.lintchecks.detectors

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.model.LintModelLibrary
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.visitor.AbstractUastVisitor
import ru.otus.homework.lintchecks.VIEW_MODEL
import ru.otus.homework.lintchecks.VIEW_MODEL_ARTIFACT
import ru.otus.homework.lintchecks.VIEW_MODEL_SCOPE
import ru.otus.homework.lintchecks.isContainsInClassPath
import ru.otus.homework.lintchecks.isExtendsOfClass

const val JOB_IN_BUILDER_ISSUE_ID = "JobInBuilderUsage"
const val JOB_IN_BUILDER_ISSUE_BRIEF_DESCRIPTION =
    "briefDescription - Don't use jobs in parameters coroutine builders"
const val JOB_IN_BUILDER_ISSUE_EXPLANATION =
    "explanation - Don't use jobs in parameters coroutine builders"
const val COROUTINE_SCOPE = "kotlinx.coroutines.CoroutineScope"
const val JOB = "kotlinx.coroutines.Job"
const val SUPERVISOR_JOB = "SupervisorJob"
const val NON_CANCELLABLE = "kotlinx.coroutines.NonCancellable"

@Suppress("UnstableApiUsage")
class JobInBuilderUsageDetector : Detector(), Detector.UastScanner {

    companion object {
        val ISSUE = Issue.create(
            id = JOB_IN_BUILDER_ISSUE_ID,
            briefDescription = JOB_IN_BUILDER_ISSUE_BRIEF_DESCRIPTION,
            explanation = JOB_IN_BUILDER_ISSUE_EXPLANATION,
            implementation = Implementation(
                JobInBuilderUsageDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            ),
            category = Category.PERFORMANCE,
            severity = Severity.FATAL
        )
    }

    override fun getApplicableMethodNames(): List<String> {
        return listOf("launch", "async")
    }

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val psiClassOfReceiver = context.evaluator.getTypeClass(node.receiverType)
        val isInvokeMethodOnCoroutineScope = context.evaluator.inheritsFrom(
            psiClassOfReceiver,
            COROUTINE_SCOPE,
            false
        )

        if (isInvokeMethodOnCoroutineScope) {
            node.valueArguments.forEach { argumentOfCoroutineBuilderMethod ->
                if (argumentOfCoroutineBuilderMethod !is ULambdaExpression) {
                    argumentOfCoroutineBuilderMethod.accept(
                        ParameterMethodVisitor(context, node, fix(), getApplicableMethodNames())
                    )
                }
            }
        }
    }

    private class ParameterMethodVisitor(
        private val context: JavaContext,
        private val coroutineBuilderMethod: UCallExpression,
        private val lintFix: LintFix.Builder,
        private val applicableCoroutineBuilderMethodNames: List<String>
    ) : AbstractUastVisitor() {

        override fun visitElement(node: UElement): Boolean {
            if (node is USimpleNameReferenceExpression) {
                val psiClassOfMethodParameter =
                    context.evaluator.getTypeClass(node.getExpressionType())
                val isInheritsFromJob =
                    context.evaluator.inheritsFrom(psiClassOfMethodParameter, JOB, false)

                if (isInheritsFromJob) {
                    report(coroutineBuilderMethod, node)
                }
            }
            return false
        }

        private fun report(
            coroutineBuilderMethod: UCallExpression,
            jobParameterOfMethod: USimpleNameReferenceExpression
        ) {
            val location = context.getLocation(jobParameterOfMethod)
            val reportSettings = createReportSettings(
                location,
                context,
                coroutineBuilderMethod,
                jobParameterOfMethod
            )

            context.report(
                ISSUE,
                jobParameterOfMethod,
                location,
                reportSettings.description,
                reportSettings.lintFix
            )
        }

        private fun createReportSettings(
            location: Location,
            context: JavaContext,
            coroutineBuilderMethod: UCallExpression,
            jobParameterOfMethod: USimpleNameReferenceExpression
        ): ReportSettings {
            val psiTypeJobParameterOfMethod = jobParameterOfMethod.getExpressionType()
            val psiClassJobParameterOfMethod =
                context.evaluator.getTypeClass(psiTypeJobParameterOfMethod)
            val parameterName = jobParameterOfMethod.sourcePsi?.text.orEmpty()
            val dependencies = context.evaluator.dependencies?.getAll()
            val containingClassOfMethodInvocation = jobParameterOfMethod.getContainingUClass()

            return when {
                isSupervisorCondition(
                    parameterName,
                    coroutineBuilderMethod,
                    dependencies,
                    context,
                    containingClassOfMethodInvocation
                ) -> {
                    ReportSettings(
                        JOB_IN_BUILDER_ISSUE_BRIEF_DESCRIPTION,
                        createSupervisorJobFix(location)
                    )
                }
                isNonCancellableCondition(
                    psiTypeJobParameterOfMethod,
                    coroutineBuilderMethod.uastParent
                ) -> {
                    val methodLocation =
                        context.getLocation(coroutineBuilderMethod.methodIdentifier)
                    ReportSettings(
                        JOB_IN_BUILDER_ISSUE_BRIEF_DESCRIPTION,
                        createNonCancellableFix(methodLocation)
                    )
                }
                isOtherCondition(context, containingClassOfMethodInvocation) -> ReportSettings(
                    JOB_IN_BUILDER_ISSUE_BRIEF_DESCRIPTION,
                    null
                )
                else -> {
                    ReportSettings(
                        JOB_IN_BUILDER_ISSUE_BRIEF_DESCRIPTION,
                        null
                    )
                }
            }
        }

        private fun isSupervisorCondition(
            name: String,
            uCallExpression: UCallExpression,
            dependencies: List<LintModelLibrary>?,
            context: JavaContext,
            uContainingClass: UClass?
        ) = name.contains(SUPERVISOR_JOB) && isInvokeOnViewModelScope(uCallExpression)
                && isContainsInClassPath(dependencies, VIEW_MODEL_ARTIFACT)
                && isExtendsOfClass(
            context = context,
            containingUClass = uContainingClass,
            expectedExtendClass = VIEW_MODEL
        )

        private fun isInvokeOnViewModelScope(uCallExpression: UCallExpression): Boolean =
            uCallExpression.receiver?.sourcePsi?.text == VIEW_MODEL_SCOPE &&
                    uCallExpression.receiverType?.canonicalText == COROUTINE_SCOPE

        private fun createSupervisorJobFix(location: Location): LintFix =
            lintFix
                .replace()
                .range(location)
                .all()
                .with("")
                .build()

        private fun isNonCancellableCondition(
            psiType: PsiType?,
            uElement: UElement?
        ): Boolean {
            return psiType?.canonicalText == NON_CANCELLABLE &&
                    isCoroutineBuilderInvokeInOtherCoroutineBuilder(uElement)
        }

        private tailrec fun isCoroutineBuilderInvokeInOtherCoroutineBuilder(
            uElement: UElement?
        ): Boolean {
            return when {
                uElement == null || uElement is UMethod -> false
                uElement is UCallExpression
                        && applicableCoroutineBuilderMethodNames.any { it == uElement.methodName } -> true
                else -> isCoroutineBuilderInvokeInOtherCoroutineBuilder(
                    uElement.uastParent
                )
            }
        }

        private fun createNonCancellableFix(location: Location): LintFix =
            lintFix
                .replace()
                .range(location)
                .all()
                .with("withContext")
                .build()

        private fun isOtherCondition(
            context: JavaContext,
            uContainingClass: UClass?
        ) = !isExtendsOfClass(
            context = context,
            containingUClass = uContainingClass,
            expectedExtendClass = VIEW_MODEL
        )

        private class ReportSettings(
            val description: String,
            val lintFix: LintFix?
        )
    }
}