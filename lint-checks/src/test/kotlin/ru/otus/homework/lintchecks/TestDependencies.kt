package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestFile

@Suppress("UnstableApiUsage")
val kotlinxCoroutines: TestFile = kotlin(
    """
    package kotlinx.coroutines
    import kotlin.coroutines.CoroutineContext

    interface Job : CoroutineContext
    interface CompletableJob : Job
    interface CoroutineScope {
        val coroutineContext: CoroutineContext
    }
    class JobStub : Job {
        override fun plus(context: CoroutineContext): CoroutineContext = CoroutineContextStub()
    }
    fun CoroutineScope(context: CoroutineContext): CoroutineScope = JobStub()
    fun CoroutineScope.launch(
        context: CoroutineContext,
        block: suspend CoroutineScope.() -> Unit
    ): Job = JobStub()
    fun SupervisorJob(parent: Job? = null): CompletableJob = JobStub()
    fun Job(parent: Job? = null): Job = JobStub()
    """.trimIndent()
)

@Suppress("UnstableApiUsage")
val androidxLifecycle: TestFile = kotlin(
    """
    package androidx.lifecycle
    import kotlinx.coroutines.CoroutineScope

    abstract class ViewModel
    val ViewModel.viewModelScope: CoroutineScope = CoroutineScopeStub()
    """.trimIndent()
)
