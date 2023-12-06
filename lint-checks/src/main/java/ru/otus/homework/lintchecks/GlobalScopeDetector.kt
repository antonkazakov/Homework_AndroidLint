package ru.otus.homework.lintchecks

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.getParentOfType

private const val ID = "GlobalScopeUsage"
private const val BRIEF_DESCRIPTION = "Не используйте GlobalScope"
private const val EXPLANATION = """Корутины, запущенные на kotlinx.coroutines.GlobalScope нужно контролировать вне 
    |скоупа класса, в котором они созданы. Контролировать глобальные корутины неудобно, а отсутствие контроля может 
    |привести к излишнему использованию ресурсов и утечкам памяти."""

class GlobalScopeDetector : Detector(), Detector.UastScanner {

    companion object {
        val ISSUE = Issue.create(
            id = ID,
            briefDescription = BRIEF_DESCRIPTION,
            explanation = EXPLANATION,
            category = Category.CORRECTNESS,
            priority = 6,
            severity = Severity.WARNING,
            implementation = Implementation(
                GlobalScopeDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }

    override fun getApplicableUastTypes(): List<Class<out UElement>> {
        return listOf(UCallExpression::class.java)
    }

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {
            override fun visitCallExpression(node: UCallExpression) {
                if (node.receiverType?.canonicalText == "kotlinx.coroutines.GlobalScope") {
                    context.report(
                        ISSUE,
                        node,
                        context.getLocation(node),
                        BRIEF_DESCRIPTION,
                        quickFix(context, node)
                    )
                }
            }
        }
    }

    private fun quickFix(context: JavaContext, node: UCallExpression): LintFix? {
        return when {
            canReplaceWithViewModelScope(context, node) -> createReplaceLintFix("viewModelScope")
            canReplaceWithLifecycleScope(context, node) -> createReplaceLintFix("lifecycleScope")
            else -> null
        }
    }

    private fun createReplaceLintFix(result: String): LintFix {
        return LintFix.create()
            .replace()
            .text("GlobalScope")
            .with(result)
            .build()
    }

    private fun canReplaceWithViewModelScope(context: JavaContext, node: UCallExpression): Boolean {
        return context.evaluator.extendsClass(node.getParentOfType<UClass>(), "androidx.lifecycle.ViewModel") &&
                context.evaluator.dependencies?.packageDependencies?.roots
                    ?.any { it.artifactName == "androidx.lifecycle:lifecycle-viewmodel-ktx" } ?: false
    }

    private fun canReplaceWithLifecycleScope(context: JavaContext, node: UCallExpression): Boolean {
        return context.evaluator.extendsClass(node.getParentOfType<UClass>(), "androidx.fragment.app.Fragment") &&
                context.evaluator.dependencies?.packageDependencies?.roots
                    ?.any { it.artifactName == "androidx.lifecycle:lifecycle-runtime-ktx" } ?: false
    }
}