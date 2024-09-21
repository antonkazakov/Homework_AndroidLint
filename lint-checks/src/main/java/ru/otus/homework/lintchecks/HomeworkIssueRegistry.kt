package ru.otus.homework.lintchecks

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue
import ru.otus.homework.lintchecks.color.ColorIssue
import ru.otus.homework.lintchecks.globalscope.GlobalScopeIssue
import ru.otus.homework.lintchecks.jobbuilder.JobBuilderIssue

@Suppress("UnstableApiUsage")
class HomeworkIssueRegistry : IssueRegistry() {

    override val issues: List<Issue> =
        listOf(
            GlobalScopeIssue.ISSUE,
            JobBuilderIssue.ISSUE,
            ColorIssue.ISSUE
        )

    override val api: Int
        get() = CURRENT_API
}