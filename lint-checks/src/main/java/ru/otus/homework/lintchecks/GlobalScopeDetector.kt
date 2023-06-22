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
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.uast.UElement
import org.jetbrains.uast.USimpleNameReferenceExpression

private const val s = "o"

private const val ID = "GlobalScopeUsage"
private const val BRIEF_DESCRIPTION =
    "Замените GlobalScope на другой CoroutineScope"
private const val EXPLANATION =
    "Замените GlobalScope на другой CoroutineScope. " +
            "Использование GlobalScope может привести к излишнему использованию ресурсов и утечкам памяти"

private const val PRIORITY = 6

private const val CLASS_GLOBAL_SCOPE = "kotlinx.coroutines.GlobalScope"
private const val CLASS_VIEW_MODEL = "androidx.lifecycle.ViewModel"
private const val CLASS_FRAGMENT = "androidx.fragment.app.Fragment"

private const val NODE_GLOBAL_SCOPE = "GlobalScope"

private const val ARTIFACT_VIEW_MODEL = "androidx.lifecycle:lifecycle-viewmodel-ktx"
private const val ARTIFACT_FRAGMENT = "androidx.lifecycle:lifecycle-runtime-ktx"

private const val REPLACEMENT_LIFECYCLE_SCOPE = "lifecycleScope"
private const val REPLACEMENT_VIEW_MODEL_SCOPE = "viewModelScope"
private const val REPLACEMENT_COROUTINE_SCOPE = "CoroutineScope(Dispatchers.Default)"

class GlobalScopeDetector : Detector(), Detector.UastScanner {

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
            if (!(node.identifier == NODE_GLOBAL_SCOPE && node.getExpressionType()?.canonicalText == CLASS_GLOBAL_SCOPE)) {
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
                    CLASS_VIEW_MODEL,
                    ARTIFACT_VIEW_MODEL
                )
            ) {
                superClassType = SuperClassType.VIEW_MODEL
            } else if (
                hasParentClassAndArtifact(
                    context,
                    ktClass?.toLightClass()?.superTypes ?: emptyArray(),
                    CLASS_FRAGMENT,
                    ARTIFACT_FRAGMENT
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

    private fun createFix(
        className: String?,
        superClassType: SuperClassType,
        location: Location
    ): LintFix {
        if (className == null) return replaceWithOtherScope(superClassType)
        return fix().alternatives(
            replaceWithOtherScope(superClassType),
            createReplaceFix(location, superClassType)
        )
    }

    private fun getReplacement(superClassType: SuperClassType) = when (superClassType) {
        SuperClassType.FRAGMENT -> REPLACEMENT_LIFECYCLE_SCOPE
        SuperClassType.VIEW_MODEL -> REPLACEMENT_VIEW_MODEL_SCOPE
        else -> REPLACEMENT_COROUTINE_SCOPE
    }

    private fun replaceWithOtherScope(superClassType: SuperClassType): LintFix {
        return fix().replace()
            .text(NODE_GLOBAL_SCOPE)
            .with(getReplacement(superClassType))
            .build()
    }

    private fun createReplaceFix(
        location: Location,
        superClassType: SuperClassType
    ): LintFix {
        return fix().replace()
            .range(location)
            .text(NODE_GLOBAL_SCOPE)
            .with(getReplacement(superClassType))
            .build()
    }

    private enum class SuperClassType {
        VIEW_MODEL, FRAGMENT, OTHER
    }
}
