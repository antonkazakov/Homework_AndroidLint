package ru.otus.homework.lintchecks

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.Issue
import ru.otus.homework.lintchecks.detectors.GlobalScopeUsagesDetector
import ru.otus.homework.lintchecks.detectors.JobInBuilderUsageDetector

class HomeworkIssueRegistry : IssueRegistry() {

    override val issues: List<Issue>
        get() = listOf(
            GlobalScopeUsagesDetector.ISSUE,
            JobInBuilderUsageDetector.ISSUE
        )
}