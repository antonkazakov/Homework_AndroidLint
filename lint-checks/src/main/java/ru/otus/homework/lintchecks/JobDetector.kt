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
                        scope = node,
                        location = context.getLocation(node),
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

        if (viewModelElement != null) {
            return createViewModelScopeFix(context, node)
        }

        return null
    }

    private fun createViewModelScopeFix(
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