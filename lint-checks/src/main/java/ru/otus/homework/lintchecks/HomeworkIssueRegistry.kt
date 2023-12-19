package ru.otus.homework.lintchecks

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.CURRENT_API

class HomeworkIssueRegistry : IssueRegistry() {

    override val issues = listOf(
        GlobalScopeDetector.ISSUE,
        JobInBuilderDetector.ISSUE,
        ColorDetector.ISSUE,
    )

    override val api = CURRENT_API
}
