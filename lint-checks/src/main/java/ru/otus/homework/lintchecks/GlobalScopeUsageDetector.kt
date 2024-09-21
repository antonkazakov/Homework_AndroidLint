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
class GlobalScopeUsageDetector : Detector(), Detector.UastScanner {

    companion object {
        private const val ISSUE_ID = "GlobalScopeUsage"
        private const val BRIEF_DESCRIPTION = "Avoid using GlobalScope"
        private const val EXPLANATION = """
            GlobalScope should be avoided as it creates global coroutines that are not tied to 
            any lifecycle and can lead to resource leaks. Consider using a CoroutineScope tied 
            to a lifecycle or a ViewModelScope.
        """

        private const val VIEWMODEL_CLASS = "androidx.lifecycle.ViewModel"
        private const val FRAGMENT_CLASS = "androidx.fragment.app.Fragment"

        private const val VIEWMODEL_ARTIFACT = "androidx.lifecycle:lifecycle-viewmodel-ktx"
        private const val LIFECYCLE_RUNTIME_ARTIFACT = "androidx.lifecycle:lifecycle-runtime-ktx"

        private const val GLOBAL_SCOPE = "GlobalScope"
        private const val VIEWMODEL_SCOPE = "viewModelScope"
        private const val LIFECYCLE_SCOPE = "lifecycleScope"

        val ISSUE = Issue.create(
            id = ISSUE_ID,
            briefDescription = BRIEF_DESCRIPTION,
            explanation = EXPLANATION,
            category = Category.CORRECTNESS,
            priority = 6,
            severity = Severity.WARNING,
            implementation = Implementation(
                GlobalScopeUsageDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }

    override fun getApplicableUastTypes(): List<Class<out UElement>>? {
        return listOf(USimpleNameReferenceExpression::class.java)
    }

    override fun createUastHandler(context: JavaContext): UElementHandler? {
        return GlobalScopeUsageHandler(context)
    }

    class GlobalScopeUsageHandler(private val context: JavaContext) : UElementHandler() {

        override fun visitSimpleNameReferenceExpression(node: USimpleNameReferenceExpression) {
            if (node.identifier == GLOBAL_SCOPE) {

                val containingClass = node.getContainingUClass()?.javaPsi

                val fix = when {
                    containingClass?.isSubclassOf(
                        context = context,
                        className = VIEWMODEL_CLASS
                    ) == true && hasArtifact(
                        context = context,
                        artifactName = VIEWMODEL_ARTIFACT
                    ) -> createFix(node, VIEWMODEL_SCOPE)

                    containingClass?.isSubclassOf(
                        context = context,
                        className = FRAGMENT_CLASS,
                    ) == true && hasArtifact(
                        context = context,
                        artifactName = LIFECYCLE_RUNTIME_ARTIFACT
                    ) -> createFix(node, LIFECYCLE_SCOPE)

                    else -> null
                }

                context.report(
                    issue = ISSUE,
                    scope = node,
                    location = context.getLocation(node),
                    message = BRIEF_DESCRIPTION,
                    quickfixData = fix
                )
            }
        }

        private fun createFix(node: USimpleNameReferenceExpression, replacement: String): LintFix {
            return LintFix.create()
                .replace()
                .text(GLOBAL_SCOPE)
                .with(replacement)
                .range(context.getLocation(node))
                .shortenNames()
                .build()
        }
    }
}