package ru.otus.homework.lintchecks

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import org.jetbrains.uast.UElement
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.getContainingUClass

@Suppress("UnstableApiUsage")
class GlobalScopeDetector : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> {
        return listOf<Class<out UElement>>(USimpleNameReferenceExpression::class.java)
    }

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {

            override fun visitSimpleNameReferenceExpression(node: USimpleNameReferenceExpression) {
                if (node.identifier == GLOBAL_SCOPE) {
                    context.report(
                        issue = ISSUE,
                        scope = node,
                        location = context.getLocation(node),
                        message = BRIEF,
                        quickfixData = createFix(context, node)
                    )
                }
            }
        }
    }

    private fun createFix(
        context: JavaContext,
        node: USimpleNameReferenceExpression
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
             return createViewModelScopeFix(context)
        }

        val fragmentElement = node.getContainingUClass()?.javaPsi
            ?.find(
                iterator = { it.superClass },
                predicate = { psiClass ->
                    psiClass.name == "Fragment" &&
                            context.evaluator.getPackage(psiClass)?.qualifiedName == "androidx.fragment.app"
                }
            )

        if (fragmentElement != null) {
            return createFragmentScopeFix(context)
        }

        return null
    }

    private fun createViewModelScopeFix(
        context: JavaContext
    ): LintFix? {
        val containViewModelArtifact = context.evaluator.dependencies?.getAll()?.any {
            it.identifier.contains("androidx.lifecycle:lifecycle-viewmodel-kt")
        }

        if (containViewModelArtifact != true) return null

        return fix()
            .replace()
            .text(GLOBAL_SCOPE)
            .with("viewModelScope")
            .shortenNames()
            .reformat(true)
            .build()
    }

    private fun createFragmentScopeFix(
        context: JavaContext
    ): LintFix? {
        val containViewModelArtifact = context.evaluator.dependencies?.getAll()?.any {
            it.identifier.contains("androidx.lifecycle:lifecycle-runtime-ktx")
        }

        if (containViewModelArtifact != true) return null

        return fix()
            .replace()
            .text(GLOBAL_SCOPE)
            .with("viewLifecycleOwner.lifecycleScope")
            .shortenNames()
            .reformat(true)
            .build()
    }

    companion object {

        private const val BRIEF = "brief description"
        private const val EXPLANATION = "explanation"
        private const val ID = "GlobalScopeUsage"
        private const val GLOBAL_SCOPE = "GlobalScope"

        val ISSUE = Issue.create(
            id = ID,
            briefDescription = BRIEF,
            explanation = EXPLANATION,
            category = Category.LINT,
            priority = 3,
            severity = Severity.ERROR,
            implementation = Implementation(GlobalScopeDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )

    }

}