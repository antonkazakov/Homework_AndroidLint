package ru.otus.homework.lintchecks

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.Issue
import ru.otus.homework.lintchecks.detector.GlobalScopeDetector
import ru.otus.homework.lintchecks.detector.JobInBuilderDetector
import ru.otus.homework.lintchecks.detector.RawColorDetector

@Suppress("UnstableApiUsage")
class HomeworkIssueRegistry : IssueRegistry() {

    override val issues: List<Issue> = listOf(
        GlobalScopeDetector.ISSUE,
        JobInBuilderDetector.ISSUE,
        RawColorDetector.ISSUE
    )
}