package ru.otus.homework.lintchecks

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
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.getContainingUClass

private const val ID = "GlobalScopeUsage"
private const val BRIEF_DESCRIPTION =
    "Замените GlobalScope на другой CoroutineScope"
private const val EXPLANATION =
    "Замените GlobalScope на другой CoroutineScope. " +
            "Использование GlobalScope может привести к излишнему использованию ресурсов и утечкам памяти"

private const val PRIORITY = 6

private const val CLASS = "kotlinx.coroutines.GlobalScope"

class GlobalScopeDetector: Detector(), Detector.UastScanner {

    companion object {

        val ISSUE = Issue.create(
            ID,
            BRIEF_DESCRIPTION,
            EXPLANATION,
            Category.create("TEST CATEGORY", 10),
            PRIORITY,
            Severity.WARNING,
            Implementation(GlobalScopeDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }

    override fun getApplicableUastTypes(): List<Class<out UElement>>? {
        return listOf(USimpleNameReferenceExpression::class.java)
    }

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {
        override fun visitSimpleNameReferenceExpression(node: USimpleNameReferenceExpression) {
            if (!(node.identifier == "GlobalScope" && node.getExpressionType()?.canonicalText == CLASS)) {
                return
            }

            val psiElement = node.sourcePsi

            val ktClass = psiElement?.getParentOfType<KtClass>(true)
            val className = ktClass?.name

            var superClassType = SuperClassType.OTHER

            if (
                hasParentClassAndArtifact(
                    context,
                    ktClass?.toLightClass()?.superTypes ?: emptyArray(),
                    "androidx.lifecycle.ViewModel",
                    "androidx.lifecycle:lifecycle-viewmodel-ktx"
                )
            ) {
                superClassType = SuperClassType.VIEW_MODEL
            } else if (
                hasParentClassAndArtifact(
                    context,
                    ktClass?.toLightClass()?.superTypes ?: emptyArray(),
                    "androidx.fragment.app.Fragment",
                    "androidx.lifecycle:lifecycle-runtime-ktx"
                )
            ) {
                superClassType = SuperClassType.FRAGMENT
            }

            context.report(
                ISSUE,
                node,
                context.getLocation(node),
                BRIEF_DESCRIPTION,
                createFix(className, superClassType, context.getLocation(node))
            )
        }
    }

    private fun hasParentClassAndArtifact(context: JavaContext, superTypes: Array<out PsiType>, canonicalClassName: String, artifact: String): Boolean {
        superTypes.forEach {
            val hasDependency = context.evaluator.dependencies?.getAll()?.any { it.identifier.lowercase().contains(artifact) } == true
            return if (it.canonicalText == canonicalClassName && hasDependency) {
                true
            } else {
                hasParentClassAndArtifact(context, it.superTypes ?: emptyArray(), canonicalClassName, artifact)
            }
        }
        return false
    }

    private fun createFix(className: String?, superClassType: SuperClassType, location: Location): LintFix {
        if (className == null) return replaceWithOtherScope(superClassType)
        return fix().alternatives(
            replaceWithOtherScope(superClassType),
            createReplaceFix(location, superClassType)
        )
    }

    private fun getReplacement(superClassType: SuperClassType) = when (superClassType) {
        SuperClassType.FRAGMENT -> "lifecycleScope"
        SuperClassType.VIEW_MODEL -> "viewModelScope"
        else -> "CoroutineScope(Dispatchers.Default)"
    }

    private fun replaceWithOtherScope(superClassType: SuperClassType): LintFix {
        return fix().replace()
            .text("GlobalScope")
            .with(getReplacement(superClassType))
            .build()
    }

    private fun createReplaceFix(
        location: Location,
        superClassType: SuperClassType
    ): LintFix {
        return fix().replace()
            .range(location)
            .text("GlobalScope")
            .with(getReplacement(superClassType))
            .build()
    }

    private enum class SuperClassType {
        VIEW_MODEL, FRAGMENT, OTHER
    }
}
