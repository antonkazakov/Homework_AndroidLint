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
import com.intellij.psi.PsiClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.getContainingUClass


class GlobalScopeUsageDetector : Detector(), Detector.UastScanner {
    // Specifies the types of UElements that this detector is applicable to
    override fun getApplicableUastTypes(): List<Class<out UElement>> {
        return listOf<Class<out UElement>>(USimpleNameReferenceExpression::class.java)
    }

    // Creates a handler to analyze UElements
    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {
            // Overrides the visit method for simple name reference expressions
            override fun visitSimpleNameReferenceExpression(node: USimpleNameReferenceExpression) {
                // Check if the identifier matches "GlobalScope"
                if (node.identifier == GLOBAL_SCOPE) {
                    // Report the usage of GlobalScope
                    context.report(
                        issue = ISSUE,
                        scope = node,
                        location = context.getLocation(node),
                        message = BRIEF_DESCRIPTION,
                        quickfixData = quickFix(context, node)
                    )
                }
            }
        }
    }

    // Provides quick fixes based on the context of the node
    private fun quickFix(
        context: JavaContext,
        node: USimpleNameReferenceExpression
    ): LintFix? {
        // Get the enclosing class of the node
        val psiClass = node.getContainingUClass()?.javaPsi
        // Check if the enclosing class is a ViewModel and return a relevant fix
        if (psiClass?.hasParent(context, ANDROIDX_LIFECYCLE_VIEW_MODEL) == true) {
            return quickViewModelScopeFix(
                context, node
            )
        }
        // Check if the enclosing class is a Fragment and return a relevant fix
        if (psiClass?.hasParent(context, ANDROIDX_FRAGMENT_APP_FRAGMENT) == true) {
            return quickFragmentScopeFix(context, node)
        }
        // Return null if no applicable fixes are found
        return null
    }

    // Generate a fix for replacing GlobalScope with viewModelScope
    private fun quickViewModelScopeFix(
        context: JavaContext,
        node: USimpleNameReferenceExpression
    ): LintFix? {
        // Check if the ViewModel KTX dependency is present
        val hasViewModelArtifact = hasArtifact(
            context = context,
            artifactName = ANDROIDX_LIFECYCLE_LIFECYCLE_VIEW_MODEL_KTX
        )
        // Return null if the dependency is missing
        if (!hasViewModelArtifact) return null

        // Build and return the fix to replace GlobalScope with viewModelScope
        return fix()
            .replace()
            .range(context.getLocation(node))
            .with(VIEW_MODEL_SCOPE)
            .shortenNames()
            .reformat(true)
            .build()
    }

    // Generate a fix for replacing GlobalScope with viewLifecycleOwner.lifecycleScope
    private fun quickFragmentScopeFix(
        context: JavaContext,
        node: USimpleNameReferenceExpression
    ): LintFix? {
        // Check if the Fragment KTX dependency is present
        val hasFragmentArtifact = hasArtifact(
            context = context,
            artifactName = ANDROIDX_LIFECYCLE_LIFECYCLE_RUNTIME_KTX
        )

        // Return null if the dependency is missing
        if (!hasFragmentArtifact) return null

        // Build and return the fix to replace GlobalScope with viewLifecycleOwner.lifecycleScope
        return fix()
            .replace()
            .range(context.getLocation(node))
            .with(VIEW_LIFECYCLE_OWNER_LIFECYCLE_SCOPE)
            .shortenNames()
            .reformat(true)
            .build()
    }

    companion object {
        // Constants for scope identifiers
        private const val VIEW_MODEL_SCOPE = "viewModelScope"
        private const val VIEW_LIFECYCLE_OWNER_LIFECYCLE_SCOPE = "viewLifecycleOwner.lifecycleScope"
        private const val GLOBAL_SCOPE = "GlobalScope"

        // Constants for identifying dependencies and scopes
        private const val ANDROIDX_LIFECYCLE_VIEW_MODEL = "androidx.lifecycle.ViewModel"
        private const val ANDROIDX_FRAGMENT_APP_FRAGMENT = "androidx.fragment.app.Fragment"
        private const val ANDROIDX_LIFECYCLE_LIFECYCLE_VIEW_MODEL_KTX =
            "androidx.lifecycle:lifecycle-viewmodel-ktx"
        private const val ANDROIDX_LIFECYCLE_LIFECYCLE_RUNTIME_KTX =
            "androidx.lifecycle:lifecycle-runtime-ktx"


        // Metadata for the lint issue
        private const val ID = "GlobalScopeUsage"
        private const val BRIEF_DESCRIPTION =
            "не используйте GlobalScope https://elizarov.medium.com/the-reason-to-avoid-globalscope-835337445abc"
        private const val EXPLANATION = """
                GlobalScope.launch creates global coroutines. It is now developer’s responsibility to keep track of their lifetime.
                Instead, use viewModelScope in ViewModel or lifecycleScope in Fragment.
            """

        // Creating a lint issue to report when GlobalScope is used
        val ISSUE = Issue.create(
            id = ID,
            briefDescription = BRIEF_DESCRIPTION,
            explanation = EXPLANATION,
            category = Category.CORRECTNESS,
            priority = 5,
            severity = Severity.WARNING,
            implementation = Implementation(GlobalScopeUsageDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }
}

fun PsiClass.hasParent(context: JavaContext, parentName: String): Boolean {
    val className = parentName.substringAfterLast(".")
    val classPackage = parentName.substringBeforeLast(".")
    var psiClass = this
    // Traversing up the class hierarchy to find the parent
    while (psiClass.name != className) {
        psiClass = psiClass.superClass ?: return false
    }
    // Verify if the found class belongs to the specified package
    return context.evaluator.getPackage(psiClass)?.qualifiedName == classPackage
}

// Function to check if a specific artifact is included in the dependencies
fun hasArtifact(context: JavaContext, artifactName: String): Boolean {
    return context.evaluator.dependencies?.getAll()?.any {
        it.identifier.contains(artifactName)
    } == true
}
