package ru.otus.homework.lintchecks.detector

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity

@Suppress("UnstableApiUsage")
object GlobalScopeIssue {
    const val GLOBAL_SCOPE = "GlobalScope"
    private const val ID = "GlobalScopeUsage"
    const val BRIEF_DESCRIPTION =
        "Replace 'GlobalScope' to other 'CoroutineScope'"
    private const val EXPLANATION =
        "Корутины, запущенные на kotlinx.coroutines.GlobalScope нужно контролировать вне скоупа класс, в котором они созданы. Контролировать глобальные корутины неудобно, а отсутствие контроля может привести к излишнему использованию ресурсов и утечкам памяти."
    private const val PRIORITY = 6

    val ISSUE = Issue.create(
        id = ID,
        briefDescription = BRIEF_DESCRIPTION,
        explanation = EXPLANATION,
        category = Category.LINT,
        priority = PRIORITY,
        severity = Severity.WARNING,
        implementation = Implementation(GlobalScopeDetector::class.java, Scope.JAVA_FILE_SCOPE)
    )
}