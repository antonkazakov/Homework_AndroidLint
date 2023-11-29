package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles

object Mock {
    val kotlinxCoroutines: TestFile = TestFiles.kotlin(
        """
        package kotlinx.coroutines
        
        import androidx.lifecycle.CoroutineContextStub
        import kotlin.coroutines.CoroutineContext
        
        public interface Job : CoroutineContext
        public interface CompletableJob : Job
        public interface CoroutineScope {
            val coroutineContext: CoroutineContext
        }
        class JobStub: Job {
            override fun plus(context: CoroutineContext): CoroutineContext = CoroutineContextStub()
        }
        public fun CoroutineScope.launch(
            context: CoroutineContext,
            block: suspend CoroutineScope.() -> Unit
        ): Job = JobStub()
        public suspend fun delay(time: Long)
        public fun SupervisorJob(parent: Job? = null) : CompletableJob = JobStub()
        public fun Job(parent: Job? = null): Job = JobStub()
        object Dispatchers {
            val IO = CoroutineContextStub()
        }
        public object GlobalScope : CoroutineScope
        public object NonCancellable : Job {
            override fun plus(context: CoroutineContext): CoroutineContext = CoroutineContextStub()
        }
    """.trimIndent()
    )

    val kotlinCoroutines: TestFile = TestFiles.kotlin(
        """
        package kotlin.coroutines
        
        public interface CoroutineContext {
            public operator fun plus(context: CoroutineContext): CoroutineContext
        }
    """.trimIndent()
    )

    val androidxLifecycle: TestFile = TestFiles.kotlin(
        """
        package androidx.lifecycle
        
        import kotlinx.coroutines.CoroutineScope
        import kotlin.coroutines.CoroutineContext
        
        abstract class ViewModel
        class CoroutineScopeStub: CoroutineScope {
            override val coroutineContext: CoroutineContext
                get() = CoroutineContextStub()
        }
        class CoroutineContextStub: CoroutineContext {
            override fun plus(context: CoroutineContext): CoroutineContext = CoroutineContextStub()
        }
        val ViewModel.viewModelScope: CoroutineScope
            get() = CoroutineScopeStub()
    """.trimIndent()
    )

    val colorXml: TestFile = TestFiles.xml(
        "res/values/colors.xml",
        """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <color name="purple_200">#FFBB86FC</color>
                <color name="purple_500">#FF6200EE</color>
                <color name="purple_700">#FF3700B3</color>
                <color name="teal_200">#FF03DAC5</color>
                <color name="teal_700">#FF018786</color>
                <color name="black">#FF000000</color>
                <color name="white">#FFFFFFFF</color>
            </resources>
        """.trimIndent()
    )
}
