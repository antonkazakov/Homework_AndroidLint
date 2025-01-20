package ru.otus.homework.lintchecks.global_scope

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.JavaContext
import org.jetbrains.uast.USimpleNameReferenceExpression

@Suppress("UnstableApiUsage")
class GlobalScopeVisitor(private val context: JavaContext) : UElementHandler() {

    companion object{
        private const val GLOBAL_SCOPE = "GlobalScope"
    }

    override fun visitSimpleNameReferenceExpression(node: USimpleNameReferenceExpression) {
        if (node.identifier == GLOBAL_SCOPE) reportIssue(node)
    }

    private fun reportIssue(node: USimpleNameReferenceExpression) {
        context.report(
            issue = GlobalScopeIssue.ISSUE,
            scope = node,
            location = context.getNameLocation(node),
            message = GlobalScopeIssue.BRIEF_DESCRIPTION
        )
    }
}