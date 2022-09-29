package ru.otus.homework.lintchecks.detectors

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.getContainingUClass


const val GLOBAL_SCOPE_USAGES_ISSUE_ID = "GlobalScopeUsage"
const val GLOBAL_SCOPE_USAGES_ISSUE_BRIEF_DESCRIPTION = "BriefDescription"
const val GLOBAL_SCOPE_USAGES_ISSUE_EXPLANATION = "Explanation"
const val FINDING_RECEIVER_TYPE_OF_CALL_EXPRESSION = "GlobalScope"

@Suppress("UnstableApiUsage")
class GlobalScopeUsagesDetector : Detector(), Detector.UastScanner {

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
                val isGlobalScopeReceiver =
                    node.receiver.sourcePsi?.text == FINDING_RECEIVER_TYPE_OF_CALL_EXPRESSION
                if (isGlobalScopeReceiver) {
                    val location = context.getLocation(node.receiver.sourcePsi)

                    val classOfElement = node.getContainingUClass()

                    val a2 = context
                        .evaluator
                        .extendsClass(
                            classOfElement,
                            "androidx.lifecycle.ViewModel",
                            false
                        )

                    context.report(
                        ISSUE,
                        node,
                        location,
                        GLOBAL_SCOPE_USAGES_ISSUE_BRIEF_DESCRIPTION
                    )
                }
            }
        }
    }
}