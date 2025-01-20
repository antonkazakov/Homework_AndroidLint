package ru.otus.homework.lintchecks.job

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity

@Suppress("UnstableApiUsage")
object JobIssue {
    private const val ID = "JobBuilderIssue"
    private const val PRIORITY = 10
    const val BRIEF_DESCRIPTION = "Not use Job/SupervisorJob in coroutine builder"
    private const val EXPLANATION = """
        Avoid using Job or SupervisorJob within coroutine builders.
        For more information, see https://medium.com/androiddevelopers/exceptions-in-coroutines-ce8da1ec060c
    """
    private val CATEGORY = Category.CORRECTNESS
    private val SEVERITY = Severity.ERROR

    val ISSUE: Issue = Issue.create(
        id = ID,
        briefDescription = BRIEF_DESCRIPTION,
        explanation = EXPLANATION,
        category = CATEGORY,
        priority = PRIORITY,
        severity = SEVERITY,
        implementation = Implementation(
            JobDetector::class.java,
            Scope.JAVA_FILE_SCOPE
        )
    )
}