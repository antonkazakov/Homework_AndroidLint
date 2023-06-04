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
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.getContainingUClass

@Suppress("UnstableApiUsage")
class GlobalScopeDetector: Detector(), Detector.UastScanner {

    /*override fun getApplicableMethodNames(): List<String> =
        listOf("launch", "async")*/
    override fun getApplicableUastTypes() = listOf(USimpleNameReferenceExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler? {

        val handler = object: UElementHandler() {
            override fun visitSimpleNameReferenceExpression(node: USimpleNameReferenceExpression) {

                val containingClass = node.getContainingUClass() ?: return

                if (node.identifier == "GlobalScope") {

                    var fix: LintFix? = null

                    val hasKtxViewModel = context.evaluator.dependencies?.getAll()?.any { it.identifier.lowercase() == "androidx.lifecycle:lifecycle-viewmodel-ktx" } == true

                    if (containingClass.supers.any { it.qualifiedName == "androidx.lifecycle.ViewModel" } && hasKtxViewModel ) {
                        fix = createViewModelFix()
                    }

                    if (containingClass.supers.any { it.qualifiedName == "androidx.fragment.app.Fragment" }) {
                        fix = createFragmentFix()
                    }

                    reportUsage(context, node, fix)
                }
            }
        }

        return handler
    }

    private fun createViewModelFix(): LintFix {
        return fix().replace().text("GlobalScope").with("viewModelScope").build()
    }

    private fun createFragmentFix(): LintFix {
        return fix().replace().text("GlobalScope").with("lifecycleScope").build()
    }

    private fun reportUsage(context: JavaContext, node: USimpleNameReferenceExpression, fix: LintFix?) {
        context.report(
            issue = ISSUE,
            scope = node,
            location = context.getLocation(node),
            message = "GlobalScope usage is forbidden.",
            quickfixData = fix
        )
    }

    companion object {

        private val IMPLEMENTATION = Implementation(
            GlobalScopeDetector::class.java,
            Scope.JAVA_FILE_SCOPE
        )

        val ISSUE: Issue = Issue
            .create(
                id = "GlobalScopeUsage",
                briefDescription = "The GlobalScope should not be used",
                explanation = """
                GlobalScope.launch creates global coroutines. It is now developer’s responsibility to keep track of their lifetime.
            """.trimIndent(),
                category = Category.CORRECTNESS,
                priority = 10,
                severity = Severity.ERROR,
                androidSpecific = true,
                implementation = IMPLEMENTATION
            )
    }

}