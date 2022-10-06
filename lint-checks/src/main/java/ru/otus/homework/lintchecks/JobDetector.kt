package ru.otus.homework.lintchecks

import com.alexey.minay.checks.find
import com.android.tools.lint.detector.api.*
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.getContainingUClass

@Suppress("UnstableApiUsage")
class JobDetector : Detector(), Detector.UastScanner {

    override fun getApplicableMethodNames(): List<String> {
        return listOf("launch", "asunch")
    }

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val receiver = context.evaluator.getTypeClass(node.receiverType)

        val isCoroutineScope = receiver
            ?.find(
                iterator = { it.superClass },
                predicate = { psiClass ->
                    psiClass.name == "CoroutineScope" &&
                            context.evaluator.getPackage(psiClass)?.qualifiedName == "kotlinx.coroutines"
                }
            ) != null

        if (isCoroutineScope) {
            node.valueArguments.forEach { arg ->
                val param = context.evaluator.getTypeClass(arg.getExpressionType())
                val isInherits =
                    context.evaluator.inheritsFrom(param, "kotlinx.coroutines.Job", false)
                if (isInherits) {
                    context.report(
                        issue = ISSUE,
                        scope = arg,
                        location = context.getLocation(arg),
                        message = BRIEF,
                        quickfixData = createFix(context, arg)
                    )
                }
            }
        }

    }

    private fun createFix(
        context: JavaContext,
        node: UExpression
    ): LintFix? {
        val viewModelElement = node.getContainingUClass()?.javaPsi
            ?.find(
                iterator = { it.superClass },
                predicate = { psiClass ->
                    psiClass.name == "ViewModel" &&
                            context.evaluator.getPackage(psiClass)?.qualifiedName == "androidx.lifecycle"
                }
            )

        val param = context.evaluator.getTypeClass(node.getExpressionType())
        val isSupervisorJob = context.evaluator
            .inheritsFrom(param, "kotlinx.coroutines.CompletableJob", false)

        if (viewModelElement != null && isSupervisorJob) {
            return createSupervisorJobFix(context, node)
        }

        val isNonCancelableJob = context.evaluator
            .inheritsFrom(param, "kotlinx.coroutines.NonCancellable", false)

        if (isNonCancelableJob) {
            return createNonCancelableJobFix(context, node)
        }

        return null
    }

    private fun createSupervisorJobFix(
        context: JavaContext,
        node: UExpression
    ): LintFix? {
        val containViewModelArtifact = context.evaluator.dependencies?.getAll()?.any {
            it.identifier.contains("androidx.lifecycle:lifecycle-viewmodel-kt")
        }

        if (containViewModelArtifact != true) return null

        return fix()
            .replace()
            .range(context.getLocation(node))
            .all()
            .with("")
            .build()
    }

    private fun createNonCancelableJobFix(
        context: JavaContext,
        node: UExpression
    ): LintFix {
        return fix()
            .replace()
            .text("launch")
            .with("withContext")
            .shortenNames()
            .reformat(true)
            .build()
    }

    companion object {

        private const val BRIEF = "brief description"
        private const val EXPLANATION = "explanation"
        private const val ID = "JobInBuilderUsage"

        val ISSUE = Issue.create(
            id = ID,
            briefDescription = BRIEF,
            explanation = EXPLANATION,
            category = Category.LINT,
            priority = 3,
            severity = Severity.ERROR,
            implementation = Implementation(JobDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )

    }

}