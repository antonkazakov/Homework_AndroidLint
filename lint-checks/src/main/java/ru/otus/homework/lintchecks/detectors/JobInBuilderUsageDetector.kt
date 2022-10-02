package ru.otus.homework.lintchecks.detectors

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
import com.android.tools.lint.model.LintModelLibrary
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
const val JOB_IN_BUILDER_ISSUE_BRIEF_DESCRIPTION = "briefDescription - JobInBuilderUsage"
const val JOB_IN_BUILDER_ISSUE_EXPLANATION = "explanation - JobInBuilderUsage"
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

    private val methodNames = listOf("launch", "async")

    override fun getApplicableUastTypes(): List<Class<out UElement>>? {
        return listOf(UCallExpression::class.java)
    }

    override fun createUastHandler(context: JavaContext): UElementHandler? {
        return object : UElementHandler() {
            override fun visitCallExpression(node: UCallExpression) {
                val classTypeOfReceiver = context.evaluator.getTypeClass(node.receiverType)
                val isInvokeMethodOnCoroutineScope = context
                    .evaluator
                    .inheritsFrom(
                        classTypeOfReceiver,
                        COROUTINE_SCOPE,
                        false
                    )

                val methodName = node.methodIdentifier?.name
                val isCorrectMethodName = methodNames.any { it == methodName }

                if (isInvokeMethodOnCoroutineScope && isCorrectMethodName) {
                    node.valueArguments.forEach { uExpression ->
                        if (uExpression !is ULambdaExpression) {
                            uExpression.accept(
                                ParameterMethodVisitor(context, node, fix(), methodNames)
                            )
                        }
                    }
                }
            }
        }
    }

    class ParameterMethodVisitor(
        private val context: JavaContext,
        private val uCallExpression: UCallExpression,
        private val lintFix: LintFix.Builder,
        private val methodNames: List<String>
    ) : AbstractUastVisitor() {
        override fun visitElement(node: UElement): Boolean {
            if (node is USimpleNameReferenceExpression) {
                val psiClass = context.evaluator.getTypeClass(node.getExpressionType())
                val isInheritsFromJob =
                    context.evaluator.inheritsFrom(psiClass, JOB, false)

                if (isInheritsFromJob) {
                    report(uCallExpression, node)
                }
            }
            return false
        }

        private fun report(
            uCallExpression: UCallExpression,
            simpleNameReferenceExpression: USimpleNameReferenceExpression
        ) {
            val location = context.getLocation(simpleNameReferenceExpression)
            val reportSettings = createReportSettings(
                location,
                context,
                uCallExpression,
                simpleNameReferenceExpression
            )

            context.report(
                ISSUE,
                simpleNameReferenceExpression,
                location,
                reportSettings.description,
                reportSettings.lintFix
            )
        }

        private fun createReportSettings(
            location: Location,
            context: JavaContext,
            uCallExpression: UCallExpression,
            simpleNameReferenceExpression: USimpleNameReferenceExpression
        ): ReportSettings {
            val psiType = simpleNameReferenceExpression.getExpressionType()
            val psiClass = context.evaluator.getTypeClass(psiType)
            val name = simpleNameReferenceExpression.sourcePsi?.text.orEmpty()
            val dependencies = context.evaluator.dependencies?.getAll()
            val uContainingClass = simpleNameReferenceExpression.getContainingUClass()

            return when {
                isSupervisorCondition(
                    name,
                    uCallExpression,
                    dependencies,
                    context,
                    uContainingClass
                ) -> {
                    ReportSettings(
                        JOB_IN_BUILDER_ISSUE_BRIEF_DESCRIPTION,
                        createSupervisorJobFix(location)
                    )
                }
                isNonCancellableCondition(psiType, uCallExpression.uastParent) -> {
                    val methodLocation = context.getLocation(uCallExpression.methodIdentifier)
                    ReportSettings(
                        JOB_IN_BUILDER_ISSUE_BRIEF_DESCRIPTION,
                        createNonCancellableFix(methodLocation)
                    )
                }
                isOtherCondition(context, uContainingClass) -> ReportSettings(
                    JOB_IN_BUILDER_ISSUE_BRIEF_DESCRIPTION,
                    null
                )
                else -> {
                    ReportSettings(
                        JOB_IN_BUILDER_ISSUE_BRIEF_DESCRIPTION,
                        null
                    )
//                    throw Exception("Не достижима")
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

        private fun isCoroutineBuilderInvokeInOtherCoroutineBuilder(
            uElement: UElement?
        ): Boolean {
            return when {
                uElement == null || uElement is UMethod -> false
                uElement is UCallExpression
                        && methodNames.any { it == uElement.methodName } -> true
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