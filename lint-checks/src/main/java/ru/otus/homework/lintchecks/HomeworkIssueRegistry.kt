package ru.otus.homework.lintchecks

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.Issue

class HomeworkIssueRegistry : IssueRegistry() {

    override val issues: List<Issue> = listOf(
        GlobalScopeRule.ISSUE,
        JobInBuilderRule.ISSUE,
        RawColorRule.ISSUE
    )
}