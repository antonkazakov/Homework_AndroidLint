package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test
import com.android.tools.lint.checks.infrastructure.TestFile

@Suppress("UnstableApiUsage")
class JobInBuilderUsageDetectorTest {

    private val lintTask: TestLintTask = lint().allowMissingSdk().issues(JobInBuilderUsageDetector.ISSUE)

    @Test
    fun testCase1() {
        lintTask
            .files(
                kotlin(
                    """
                            package ru.otus.homework.linthomework.jobinbuilderusage
                            
                            import androidx.lifecycle.ViewModel
                            import androidx.lifecycle.viewModelScope
                            import kotlinx.coroutines.Dispatchers
                            import kotlinx.coroutines.Job
                            import kotlinx.coroutines.NonCancellable
                            import kotlinx.coroutines.SupervisorJob
                            import kotlinx.coroutines.delay
                            import kotlinx.coroutines.launch
                            import kotlin.coroutines.CoroutineContext

                            class JobInBuilderTestCase(private val job: Job) : ViewModel() {
                                fun case1() {
                                    viewModelScope.launch(SupervisorJob() + Dispatchers.IO) {
                                        delay(1000)
                                        println("Hello World")
                                    }
                                }
                            }"""
                ).indented(),
                *stubs,
            )
            .run()
            .expect(
                """todo: add error text""".trimIndent()
            )
    }

    @Test
    fun testCase2() {
        lintTask
            .files(
                kotlin(
                    """
                    package ru.otus.homework.linthomework.jobinbuilderusage
    
                    import androidx.lifecycle.ViewModel
                    import androidx.lifecycle.viewModelScope
                    import kotlinx.coroutines.Job
                    import kotlinx.coroutines.launch
                    import kotlinx.coroutines.delay
                    import kotlinx.coroutines.NonCancellable

                    class JobInBuilderTestCase(private val job: Job) : ViewModel() {
                    
                        fun case2() {
                            viewModelScope.launch(Job()) {
                                delay(1000)
                                println("Hello World")
                            }
                        }
                    }
               """
                ).indented(),
                *stubs,
            )
            .run()
            .expect(
                """src/ru/otus/homework/linthomework/jobinbuilderusage/JobInBuilderTestCase.kt:13: Warning: Don't use Job in builders [JobInBuilderUsage]
        viewModelScope.launch(Job()) {
        ^
0 errors, 1 warnings""".trimIndent()
            )
    }

    @Test
    fun testCase3() {
        lintTask
            .files(
                kotlin(
                    """
                    package ru.otus.homework.linthomework.jobinbuilderusage
    
                    import androidx.lifecycle.ViewModel
                    import androidx.lifecycle.viewModelScope
                    import kotlinx.coroutines.Job
                    import kotlinx.coroutines.launch
                    import kotlinx.coroutines.delay
                    import kotlinx.coroutines.NonCancellable

                    class JobInBuilderTestCase(private val job: Job) : ViewModel() {
                        fun case3() {
                            viewModelScope.launch(job) {
                                delay(1000)
                                println("Hello World")
                            }
                        }
                    }
               """
                ).indented(),
                *stubs,
            )
            .run()
            .expect(
                """src/ru/otus/homework/linthomework/jobinbuilderusage/JobInBuilderTestCase.kt:12: Warning: Don't use Job in builders [JobInBuilderUsage]
        viewModelScope.launch(job) {
        ^
0 errors, 1 warnings""".trimIndent()
            )
    }

    @Test
    fun testCase4() {
        lintTask
            .files(
                kotlin(
                    """
                    package ru.otus.homework.linthomework.jobinbuilderusage
    
                    import androidx.lifecycle.ViewModel
                    import androidx.lifecycle.viewModelScope
                    import kotlinx.coroutines.Job
                    import kotlinx.coroutines.launch
                    import kotlinx.coroutines.delay
                    import kotlinx.coroutines.NonCancellable

                    class JobInBuilderTestCase(private val job: Job) : ViewModel() {
                       fun case4() {
                           
                            viewModelScope.launch(NonCancellable) {
                                delay(1000)
                                println("Hello World")
                            }
                        }
                    }
               """
                ).indented(),
                *stubs,
            )
            .run()
            .expect(
                """src/ru/otus/homework/linthomework/jobinbuilderusage/JobInBuilderTestCase.kt:13: Warning: Don't use Job in builders [JobInBuilderUsage]
        viewModelScope.launch(NonCancellable) {
        ^
0 errors, 1 warnings""".trimIndent()
            )
    }

    @Test
    fun testCase5() {
        lintTask
            .files(
                kotlin(
                    """
                    package ru.otus.homework.linthomework.jobinbuilderusage
    
                    import androidx.lifecycle.ViewModel
                    import androidx.lifecycle.viewModelScope
                    import kotlinx.coroutines.Job
                    import kotlinx.coroutines.launch
                    import kotlinx.coroutines.delay

                    class JobInBuilderTestCase(private val job: Job) : ViewModel() {
                       fun case5() {
                            launch(NonCancellable)
                        }

                        companion object {
                            fun launch(context : CoroutineContext) {
                                println("Hello World")
                            }
                        }
                    }
               """
                ).indented(),
                *stubs,
            )
            .run()
            .expectClean()
    }

    @Test
    fun testCase6() {
        lintTask
            .files(
                kotlin(
                    """
                package ru.otus.homework.linthomework.jobinbuilderusage

                import androidx.lifecycle.ViewModel
                import androidx.lifecycle.viewModelScope
                import kotlinx.coroutines.Job
                import kotlinx.coroutines.launch
                import kotlinx.coroutines.delay

                class JobInBuilderTestCase(private val job: Job) : ViewModel() {
                    fun case3() {
                        viewModelScope.launch(job) {
                            delay(1000)
                            println("Hello World")
                        }
                    }
                }
               """
                ).indented(),
                *stubs,
            )
            .run()
            .expect(
                """src/ru/otus/homework/linthomework/jobinbuilderusage/JobInBuilderTestCase.kt:11: Warning: Don't use Job in builders [JobInBuilderUsage]
        viewModelScope.launch(job) {
        ^
0 errors, 1 warnings""".trimIndent()
            )
    }

    private val viewModelStub: TestFile =
        kotlin(
            """
            package androidx.lifecycle
            
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.Job
            
            open class ViewModel {
                val viewModelScope: CoroutineScope = CoroutineScope(Job())
            }
            
            val ViewModel.viewModelScope: CoroutineScope
                get() = CoroutineScope(Job())
            """
        ).indented()

    private val coroutineScopeStub: TestFile =
        kotlin(
            """
            package kotlinx.coroutines
            
            class Job
            
            class CoroutineScope(context: CoroutineContext)
            
            fun CoroutineScope.launch(job: Job, block: suspend CoroutineScope.() -> Unit) {}
            
            suspend fun delay(timeMillis: Long) {}
            """
        ).indented()

    private val nonCancellableStub: TestFile =
        kotlin(
            """
        package kotlinx.coroutines

        object NonCancellable : Job {
            override val isActive: Boolean
                get() = false
            override val isCompleted: Boolean
                get() = true
            override val isCancelled: Boolean
                get() = false

            override fun start(): Boolean = false
            override fun cancel(cause: CancellationException?): Boolean = false
            override suspend fun join() {}
        }
        """
        ).indented()

    private val dispatchersStub: TestFile =
        kotlin(
            """
        package kotlinx.coroutines

        object Dispatchers {
            val IO: CoroutineContext = TODO()
        }
        """
        ).indented()

    private val supervisorJobStub: TestFile =
        kotlin(
            """
        package kotlinx.coroutines

        class SupervisorJob : Job {
            override val isActive: Boolean
                get() = false
            override val isCompleted: Boolean
                get() = true
            override val isCancelled: Boolean
                get() = false

            override fun start(): Boolean = false
            override fun cancel(cause: CancellationException?): Boolean = false
            override suspend fun join() {}
        }
        """
        ).indented()

    private val stubs =
        arrayOf(
            viewModelStub,
            coroutineScopeStub,
            nonCancellableStub,
            dispatchersStub,
            supervisorJobStub
        )
}