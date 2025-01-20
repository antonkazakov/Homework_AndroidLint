package ru.otus.homework.lintchecks.color

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.Scope

@Suppress("UnstableApiUsage")
object ColorIssue {
    private const val ID = "ColorIssue"
    private const val PRIORITY = 10
    const val BRIEF_DESCRIPTION = "Use only predefined colors"
    private const val EXPLANATION = """
        Predefined colors are located in /res/values/colors.xml.
    """
    private val CATEGORY = Category.CORRECTNESS
    private val SEVERITY = Severity.ERROR

    val ISSUE = Issue.create(
        id = ID,
        briefDescription = BRIEF_DESCRIPTION,
        explanation = EXPLANATION,
        category = CATEGORY,
        priority = PRIORITY,
        severity = SEVERITY,
        implementation = Implementation(
            ColorDetector::class.java,
            Scope.RESOURCE_FILE_SCOPE
        )
    )
}