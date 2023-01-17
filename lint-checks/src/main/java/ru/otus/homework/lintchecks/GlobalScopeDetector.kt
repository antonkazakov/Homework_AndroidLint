package ru.otus.homework.lintchecks

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import com.intellij.psi.PsiClass
import org.jetbrains.uast.*

@Suppress("UnstableApiUsage")
class GlobalScopeDetector : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> =
        listOf(USimpleNameReferenceExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {

            override fun visitSimpleNameReferenceExpression(node: USimpleNameReferenceExpression) {
                if (node.identifier == GLOBAL_SCOPE_IDENTIFIER) {
                    context.report(
                        ISSUE,
                        node,
                        context.getLocation(node),
                        BRIEF_DESCRIPTION,
                        createFix(context, node)
                    )
                }
            }
        }

    private fun createFix(context: JavaContext, node: USimpleNameReferenceExpression): LintFix? {
        val psiClass = node.getContainingUClass()?.javaPsi
        if (psiClass?.hasParent(context, VIEW_MODEL_PARENT) == true
            && hasArtifact(context, LIFECYCLE_VIEWMODEL_ARTEFACT)
        )
            return replaceScope(VIEW_MODEL_SCOPE_REPLACE)
        if (psiClass?.hasParent(context, FRAGMENT_PARENT) == true
            && hasArtifact(context, LIFECYCLE_RUNTIME_ARTEFACT)
        )
            return replaceScope(LIFECYCLE_SCOPE_REPLACE)
        return null
    }

    private fun hasArtifact(context: JavaContext, artifactName: String): Boolean =
        context.evaluator.dependencies?.getAll()?.any {
            it.identifier.contains(artifactName)
        } == true

    private fun replaceScope(replace: String): LintFix =
        fix()
            .replace()
            .text(GLOBAL_SCOPE_IDENTIFIER)
            .with(replace)
            .build()

    companion object {

        private const val ID = "GlobalScopeUsage"
        private const val BRIEF_DESCRIPTION = "GlobalScope should be used with caution."
        private const val EXPLANATION =
            "GlobalScope could lead to leak memory and is not recommended to use. Coroutines running on this scope will be performed on the whole application lifetime. Replace it by custom scope."
        private const val PRIORITY = 1
        private const val GLOBAL_SCOPE_IDENTIFIER = "GlobalScope"
        private const val VIEW_MODEL_SCOPE_REPLACE = "viewModelScope"
        private const val LIFECYCLE_SCOPE_REPLACE = "lifecycleScope"
        private const val LIFECYCLE_VIEWMODEL_ARTEFACT =
            "androidx.lifecycle:lifecycle-viewmodel-ktx"
        private const val LIFECYCLE_RUNTIME_ARTEFACT = "androidx.lifecycle:lifecycle-runtime-ktx"
        private const val VIEW_MODEL_PARENT = "androidx.lifecycle.ViewModel"
        private const val FRAGMENT_PARENT = "androidx.fragment.app.Fragment"

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

fun PsiClass.hasParent(context: JavaContext, parentName: String): Boolean {
    val className = parentName.substringAfterLast(".")
    val classPackage = parentName.substringBeforeLast(".")
    var psiClass = this
    while (psiClass.name != className) {
        psiClass = psiClass.superClass ?: return false
    }
    return context.evaluator.getPackage(psiClass)?.qualifiedName == classPackage
}