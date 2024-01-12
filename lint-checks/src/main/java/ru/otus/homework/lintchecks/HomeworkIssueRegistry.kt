package ru.otus.homework.lintchecks

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.Issue

@Suppress("UnstableApiUsage")
class HomeworkIssueRegistry : IssueRegistry() {

    override val issues: List<Issue>
        get() = listOf(
            GlobalScopeUsageDetector.ISSUE,
            JobInBuilderUsageDetector.ISSUE,
        )
}