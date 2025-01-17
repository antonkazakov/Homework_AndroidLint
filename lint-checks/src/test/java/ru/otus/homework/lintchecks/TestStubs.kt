package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestFile

val kotlinxCoroutines: TestFile = kotlin(
    """
        package kotlinx.coroutines
        import androidx.lifecycle.CoroutineContextStub
        import kotlin.coroutines.CoroutineContext
        public interface Job : CoroutineContext
        public interface CoroutineScope {
            public val coroutineContext: CoroutineContext
        }
        class JobStub: Job {
            override fun plus(context: CoroutineContext): CoroutineContext = CoroutineContextStub()
        }
        public fun CoroutineScope.launch(
            context: CoroutineContext,
            block: suspend CoroutineScope.() -> Unit
        ): Job = JobStub()
        public fun SupervisorJob(parent: Job? = null) : Job = JobStub()
        public fun Job(parent: Job? = null): Job = JobStub()
        object Dispatchers {
            val IO = CoroutineContextStub()
        }
        public object NonCancellable : Job {
            override fun plus(context: CoroutineContext): CoroutineContext = CoroutineContextStub()
        }
    """.trimIndent()
)

val kotlinCoroutines: TestFile = kotlin(
    """
        package kotlin.coroutines
        public interface CoroutineContext {
            public operator fun plus(context: CoroutineContext): CoroutineContext
        }
    """.trimIndent()
)

val androidxLifecycle: TestFile = kotlin(
    """
        package androidx.lifecycle
        import kotlinx.coroutines.CoroutineScope
        import kotlin.coroutines.CoroutineContext
        public abstract class ViewModel
        class CoroutineScopeStub: CoroutineScope {
            override val coroutineContext: CoroutineContext
                get() = CoroutineContextStub()
        }
        class CoroutineContextStub: CoroutineContext {
            override fun plus(context: CoroutineContext): CoroutineContext = CoroutineContextStub()
        }
        public val ViewModel.viewModelScope: CoroutineScope
            get() = CoroutineScopeStub()
    """.trimIndent()
)