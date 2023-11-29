package ru.otus.homework.lintchecks

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.Issue
import ru.otus.homework.lintchecks.rules.GlobalScopeUsageRule
import ru.otus.homework.lintchecks.rules.JobInBuilderUsageRule
import ru.otus.homework.lintchecks.rules.RawColorUsageRule

@Suppress("UnstableApiUsage")
class HomeworkIssueRegistry : IssueRegistry() {

    override val issues: List<Issue> = listOf(
        GlobalScopeUsageRule.ISSUE,
        JobInBuilderUsageRule.ISSUE,
        RawColorUsageRule.ISSUE
    )
}
