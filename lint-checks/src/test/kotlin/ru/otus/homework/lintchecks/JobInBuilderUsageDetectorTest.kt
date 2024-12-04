package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles
import com.android.tools.lint.checks.infrastructure.TestMode


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
            .issues(JobInBuilderUsageDetector.ISSUE)
            .testModes(TestMode.DEFAULT)
            .run()
            .expect(
                """src/ru/otus/homework/linthomework/jobinbuilderusage/JobInBuilderTestCase.kt:15: Warning: Don't use Job in builders [JobInBuilderUsage]
        viewModelScope.launch(SupervisorJob() + Dispatchers.IO) {
                              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warnings""".trimIndent()
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
                              ~~~~~
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
                              ~~~
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
                              ~~~~~~~~~~~~~~
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
                              ~~~
0 errors, 1 warnings""".trimIndent()
            )
    }

    val kotlinxCoroutinesJob: TestFile = TestFiles.kotlin(
        """
        package kotlinx.coroutines

        import androidx.lifecycle.CoroutineContextStub
        import kotlin.coroutines.CoroutineContext

        interface Job : CoroutineContext
        interface CompletableJob : Job

        class JobStub : Job {
            override fun plus(context: CoroutineContext): CoroutineContext = CoroutineContextStub()
        }

        fun SupervisorJob(parent: Job? = null): CompletableJob = JobStub()

        fun Job(parent: Job? = null): Job = JobStub()

        object NonCancellable : Job {
            override fun plus(context: CoroutineContext): CoroutineContext = CoroutineContextStub()
        }
    """.trimIndent()
    )


    val kotlinxCoroutinesScope: TestFile = TestFiles.kotlin(
        """
        package kotlinx.coroutines

        import kotlin.coroutines.CoroutineContext

        interface CoroutineScope {
            val coroutineContext: CoroutineContext
        }

        fun CoroutineScope.launch(
            context: CoroutineContext,
            block: suspend CoroutineScope.() -> Unit
        ): Job = JobStub()

        object GlobalScope : CoroutineScope
    """.trimIndent()
    )

    val kotlinxCoroutinesDispatchers: TestFile = TestFiles.kotlin(
        """
        package kotlinx.coroutines

        import androidx.lifecycle.CoroutineContextStub

        object Dispatchers {
            val IO = CoroutineContextStub()
        }
    """.trimIndent()
    )

    val kotlinxCoroutinesDelay: TestFile = TestFiles.kotlin(
        """
        package kotlinx.coroutines

        suspend fun delay(time: Long)
    """.trimIndent()
    )

    val kotlinCoroutines: TestFile = TestFiles.kotlin(
        """
        package kotlin.coroutines

        interface CoroutineContext {
            operator fun plus(context: CoroutineContext): CoroutineContext
        }
    """.trimIndent()
    )

    val androidxLifecycle: TestFile = TestFiles.kotlin(
        """
        package androidx.lifecycle

        import kotlinx.coroutines.CoroutineScope
        import kotlin.coroutines.CoroutineContext

        abstract class ViewModel

        class CoroutineScopeStub : CoroutineScope {
            override val coroutineContext: CoroutineContext
                get() = CoroutineContextStub()
        }

        class CoroutineContextStub : CoroutineContext {
            override fun plus(context: CoroutineContext): CoroutineContext = CoroutineContextStub()
        }

        val ViewModel.viewModelScope: CoroutineScope
            get() = CoroutineScopeStub()
    """.trimIndent()
    )

    private val stubs = arrayOf(
        kotlinxCoroutinesJob,
        kotlinxCoroutinesScope,
        kotlinxCoroutinesDispatchers,
        kotlinxCoroutinesDelay,
        kotlinCoroutines,
        androidxLifecycle
    )
}