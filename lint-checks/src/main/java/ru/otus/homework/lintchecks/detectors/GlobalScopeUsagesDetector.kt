package ru.otus.homework.lintchecks.detectors

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
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiClass
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.tryResolve
import ru.otus.homework.lintchecks.FRAGMENT
import ru.otus.homework.lintchecks.FRAGMENT_ARTIFACT
import ru.otus.homework.lintchecks.LIFECYCLE_SCOPE
import ru.otus.homework.lintchecks.VIEW_MODEL
import ru.otus.homework.lintchecks.VIEW_MODEL_ARTIFACT
import ru.otus.homework.lintchecks.VIEW_MODEL_SCOPE
import ru.otus.homework.lintchecks.isContainsInClassPath
import ru.otus.homework.lintchecks.isExtendsOfClass


private const val GLOBAL_SCOPE_USAGES_ISSUE_ID = "GlobalScopeUsage"
private const val GLOBAL_SCOPE_USAGES_ISSUE_BRIEF_DESCRIPTION =
    "BriefDescription - Don't use Global Scope"
private const val GLOBAL_SCOPE_USAGES_ISSUE_EXPLANATION = "Explanation - Don't use Global Scope"
private const val RECEIVER_TYPE = "kotlinx.coroutines.GlobalScope"

@Suppress("UnstableApiUsage")
class GlobalScopeUsagesDetector : Detector(), SourceCodeScanner {

    companion object {
        val ISSUE = Issue.create(
            id = GLOBAL_SCOPE_USAGES_ISSUE_ID,
            briefDescription = GLOBAL_SCOPE_USAGES_ISSUE_BRIEF_DESCRIPTION,
            explanation = GLOBAL_SCOPE_USAGES_ISSUE_EXPLANATION,
            implementation = Implementation(
                GlobalScopeUsagesDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            ),
            category = Category.PERFORMANCE,
            severity = Severity.FATAL
        )
    }

    override fun getApplicableUastTypes(): List<Class<out UElement>> {
        return listOf(UQualifiedReferenceExpression::class.java)
    }

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {

            override fun visitQualifiedReferenceExpression(node: UQualifiedReferenceExpression) {
                val receiverPsiClass = node.receiver.tryResolve() as? PsiClass
                val canonicalReceiverText =
                    context.evaluator.getClassType(receiverPsiClass)?.canonicalText

                if (canonicalReceiverText == RECEIVER_TYPE) {
                    val containingUClass = node.getContainingUClass()
                    val location = context.getLocation(node.receiver.sourcePsi)

                    context.report(
                        ISSUE,
                        node,
                        location,
                        GLOBAL_SCOPE_USAGES_ISSUE_BRIEF_DESCRIPTION,
                        createLintFix(location, context, containingUClass)
                    )
                }
            }
        }
    }

    private fun createLintFix(
        location: Location,
        context: JavaContext,
        containingUClass: UClass?
    ): LintFix? {
        val dependencies = context.evaluator.dependencies?.getAll()

        return when {
            isExtendsOfClass(context, containingUClass, VIEW_MODEL) &&
                    isContainsInClassPath(dependencies, VIEW_MODEL_ARTIFACT) -> {
                createViewModelFix(location)
            }
            isExtendsOfClass(context, containingUClass, FRAGMENT) &&
                    isContainsInClassPath(dependencies, FRAGMENT_ARTIFACT) -> {
                createFragmentFix(location)
            }
            else -> null
        }
    }

    private fun createViewModelFix(location: Location): LintFix =
        createViewFix(location, VIEW_MODEL_SCOPE)

    private fun createFragmentFix(location: Location): LintFix =
        createViewFix(location, LIFECYCLE_SCOPE)

    private fun createViewFix(location: Location, newText: String): LintFix = fix()
        .replace()
        .range(location)
        .all()
        .with(newText)
        .build()
}