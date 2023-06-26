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
                        message = BRIEF_DESCRIPTION,
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
        val psiClass = node.getContainingUClass()?.javaPsi

        if (psiClass?.hasParent(context, "androidx.lifecycle.ViewModel") == true) {
            return createViewModelScopeFix(
                context, node
            )
        }

        if (psiClass?.hasParent(context, "androidx.fragment.app.Fragment") == true) {
            return createFragmentScopeFix(context, node)
        }

        return null
    }

    private fun createViewModelScopeFix(
        context: JavaContext,
        node: USimpleNameReferenceExpression
    ): LintFix? {
        val hasViewModelArtifact =
            hasArtifact(context, "androidx.lifecycle:lifecycle-viewmodel-ktx")

        if (!hasViewModelArtifact) return null

        return fix()
            .replace()
            .range(context.getLocation(node))
            .with("viewModelScope")
            .shortenNames()
            .reformat(true)
            .build()
    }

    private fun createFragmentScopeFix(
        context: JavaContext,
        node: USimpleNameReferenceExpression
    ): LintFix? {
        val hasFragmentArtifact = hasArtifact(context, "androidx.lifecycle:lifecycle-runtime-ktx")

        if (!hasFragmentArtifact) return null

        return fix()
            .replace()
            .range(context.getLocation(node))
            .with("viewLifecycleOwner.lifecycleScope")
            .shortenNames()
            .reformat(true)
            .build()
    }

    companion object {
        private const val ID = "GlobalScopeUsage"
        private const val BRIEF_DESCRIPTION =
            "Замените 'GlobalScope' на другой вид 'CoroutineScope'"
        private const val EXPLANATION =
            "Корутины, запущенные на kotlinx.coroutines.GlobalScope нужно контролировать вне скоупа класс, в котором они созданы. Контролировать глобальные корутины неудобно, а отсутствие контроля может привести к излишнему использованию ресурсов и утечкам памяти."
        private const val GLOBAL_SCOPE = "GlobalScope"
        private const val PRIORITY = 6

        val ISSUE = Issue.create(
            id = ID,
            briefDescription = BRIEF_DESCRIPTION,
            explanation = EXPLANATION,
            category = Category.LINT,
            priority = PRIORITY,
            severity = Severity.WARNING,
            implementation = Implementation(GlobalScopeDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }
}
