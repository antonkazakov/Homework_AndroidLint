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
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.getParentOfType

@Suppress("UnstableApiUsage")
class GlobalScopeUsageDetector : Detector(), Detector.UastScanner {

    companion object {
        val ISSUE = Issue.create(
            id = ID,
            briefDescription = BRIEF_DESCRIPTION,
            explanation = EXPLANATION,
            category = Category.CUSTOM_LINT_CHECKS,
            priority = PRIORITY,
            severity = Severity.WARNING,
            implementation = Implementation(
                GlobalScopeUsageDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }

    override fun getApplicableUastTypes(): List<Class<out UElement>> {
        return listOf(UCallExpression::class.java)
    }

    override fun createUastHandler(context: JavaContext): UElementHandler? {
        return GlobalScopeHandler(context)
    }
}

private class GlobalScopeHandler(private val context: JavaContext) : UElementHandler() {
    override fun visitCallExpression(node: UCallExpression) {
        if (node.receiverType?.canonicalText == GLOBAL_SCOPE) {
            context.report(
                issue = GlobalScopeUsageDetector.ISSUE,
                scope = node,
                location = context.getLocation(node),
                message = BRIEF_DESCRIPTION,
                quickfixData = getQuickfixData(node)
            )
        }
    }

    private fun getQuickfixData(node: UCallExpression): LintFix? {
        return when {
            containsFragment(node) -> createFix(LIFECYCLE_SCOPE)
            containsViewModel(node) -> createFix(VIEWMODEL_SCOPE)
            else -> null
        }
    }

    private fun containsViewModel(node: UCallExpression): Boolean {
        return node.getParentOfType<UClass>()?.uastSuperTypes?.any {
            it.getQualifiedName().toString() == VIEWMODEL_CLASS
        } == true
                && context.evaluator.dependencies?.packageDependencies?.roots?.find {
            it.artifactName == VIEW_MODEL_KTX_ARTIFACT
        } != null
    }

    private fun containsFragment(node: UCallExpression): Boolean {
        return node.getParentOfType<UClass>()?.uastSuperTypes?.any {
            it.getQualifiedName().toString() == FRAGMENT_CLASS
        } == true
                && context.evaluator.dependencies?.packageDependencies?.roots?.find {
            it.artifactName == RUNTIME_KTX_ARTIFACT
        } != null
    }

    private fun createFix(newScope: String): LintFix {
        return LintFix.create()
            .replace()
            .text(GLOBAL_SCOPE)
            .with(newScope)
            .build()
    }
}


private const val ID = "GlobalScopeUsage"
private const val BRIEF_DESCRIPTION = "Использование GlobalScope может привести к утечкам памяти"
private const val MORE_LINK =
    "https://elizarov.medium.com/the-reason-to-avoid-globalscope-835337445abc"
private const val EXPLANATION =
    "Использование GlobalScope может привести к излишнему использованию ресурсов и утечкам памяти. Подробнее: $MORE_LINK"
private const val PRIORITY = 7
private const val GLOBAL_SCOPE = "kotlinx.coroutines.GlobalScope"

private const val VIEWMODEL_CLASS = "androidx.lifecycle.ViewModel"
private const val VIEW_MODEL_KTX_ARTIFACT = "androidx.lifecycle:lifecycle-viewmodel-ktx"
private const val FRAGMENT_CLASS = "androidx.fragment.app.Fragment"
private const val RUNTIME_KTX_ARTIFACT = "androidx.lifecycle:lifecycle-runtime-ktx"
private const val LIFECYCLE_SCOPE = "lifecycleScope"
private const val VIEWMODEL_SCOPE = "viewModelScope"