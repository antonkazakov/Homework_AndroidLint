package ru.otus.homework.lintchecks

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue
import ru.otus.homework.lintchecks.color.ColorIssue
import ru.otus.homework.lintchecks.global_scope.GlobalScopeIssue
import ru.otus.homework.lintchecks.job.JobIssue

@Suppress("UnstableApiUsage")
class HomeworkIssueRegistry : IssueRegistry() {

    override val api: Int
        get() = CURRENT_API

    override val issues: List<Issue>
        get() = listOf(
            GlobalScopeIssue.ISSUE,
            JobIssue.ISSUE,
            ColorIssue.ISSUE
        )
}