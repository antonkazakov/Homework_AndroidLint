package ru.otus.homework.lintchecks

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import org.jetbrains.uast.UClass
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.getParentOfType

private const val ID = "GlobalScopeUsage"
private const val BRIEF_DESCRIPTION = "danger GlobalScope using"
private const val GLOBAL_SCOPE = "GlobalScope"

@Suppress("UnstableApiUsage")
class GlobalScopeDetector: Detector(), Detector.UastScanner {
    companion object {
        val ISSUE = Issue.create(
            id = ID,
            briefDescription = BRIEF_DESCRIPTION,
            explanation = "using GlobalScope is danger. Please use viewModelScope or lifecycleScope",
            category = Category.create("LINT HOMEWORK", 5),
            priority = 5,
            severity = Severity.WARNING,
            implementation = Implementation(
                GlobalScopeDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            ),
            suppressAnnotations = listOf(ID),
        )
    }

    override fun getApplicableUastTypes() = listOf(USimpleNameReferenceExpression::class.java)

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {
        override fun visitSimpleNameReferenceExpression(node: USimpleNameReferenceExpression) {
            if (node.identifier != GLOBAL_SCOPE) return
            val clazzUElement = node.getParentOfType(UClass::class.java)
            var fixName: String? = null

            if (clazzUElement?.uastSuperTypes?.any {
                    it.getQualifiedName() == "androidx.lifecycle.ViewModel"
                } == true) {
                fixName = "viewModelScope"
            }
            if (clazzUElement?.uastSuperTypes?.any {
                    it.getQualifiedName() == "androidx.lifecycle:lifecycle-runtime-ktx"
                } == true) {
                fixName = "lifecycleScope"
            }

            context.report(
                issue = ISSUE,
                message = BRIEF_DESCRIPTION,
                scope = node,
                location = context.getLocation(node),
                quickfixData = if (fixName != null)
                    fix()
                        .replace()
                        .text(GLOBAL_SCOPE)
                        .with(fixName)
                        .build()
                else null
            )
        }
    }
}
