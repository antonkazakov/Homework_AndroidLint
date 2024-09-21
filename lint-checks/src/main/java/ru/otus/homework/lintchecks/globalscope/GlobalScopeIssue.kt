package ru.otus.homework.lintchecks.globalscope

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity

@Suppress("UnstableApiUsage")
object GlobalScopeIssue {
    private const val ID = "GlobalScopeIssue"
    private const val PRIORITY = 10
    const val BRIEF_DESCRIPTION = "Try to avoid GlobalScope"
    private const val EXPLANATION = """
        GlobalScope coroutine need to be kept of their lifetime.
        For more information, see https://elizarov.medium.com/the-reason-to-avoid-globalscope-835337445abc
    """
    private val CATEGORY = Category.CORRECTNESS
    private val SEVERITY = Severity.WARNING

    val ISSUE = Issue.create(
        id = ID,
        briefDescription = BRIEF_DESCRIPTION,
        explanation = EXPLANATION,
        category = CATEGORY,
        priority = PRIORITY,
        severity = SEVERITY,
        implementation = Implementation(
            GlobalScopeDetector::class.java,
            Scope.JAVA_FILE_SCOPE
        )
    )
}
