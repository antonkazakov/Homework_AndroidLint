@file:Suppress("UnstableApiUsage")

package ru.otus.homework.lintchecks

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

class HomeworkIssueRegistry : IssueRegistry() {
    override val issues: List<Issue> = listOf(
        GlobalScopeDetector.ISSUE,
        CoroutineJobDetector.ISSUE,
        ColorUsageDetector.ISSUE
    )
}