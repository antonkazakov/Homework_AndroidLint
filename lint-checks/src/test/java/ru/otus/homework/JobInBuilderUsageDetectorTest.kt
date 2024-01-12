package ru.otus.homework

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test
import ru.otus.homework.lintchecks.JobInBuilderUsageDetector

class JobInBuilderUsageDetectorTest {

    private val testLintTask = TestLintTask.lint()
        .allowMissingSdk()
        .issues(JobInBuilderUsageDetector.ISSUE)

    private val viewModelStub = LintDetectorTest.java(
        """
            package androidx.lifecycle;
        
            public abstract class ViewModel {}
        """.trimIndent()
    )

    private val viewModelExtensionsStub = LintDetectorTest.kotlin(
        """
            package androidx.lifecycle
            import kotlinx.coroutines.CoroutineScope
            public val ViewModel.viewModelScope: CoroutineScope
        """.trimIndent()
    )

    private val coroutineStub = LintDetectorTest.kotlin(
        """
            package kotlinx.coroutines
        
            public interface CoroutineScope
            fun CoroutineScope.launch(block: suspend () -> Unit) {}
            fun CoroutineScope.async(block: suspend () -> Unit) {}
            suspend fun delay(timeMillis: Long) {}
            public actual object Dispatchers {
                public val IO: Any = Any()
            }
            public interface Job {}
            public interface CompletableJob : Job{}
            private class SupervisorJobImpl(parent: Job?) : Job {  }
            public fun SupervisorJob(parent: Job? = null) : CompletableJob = SupervisorJobImpl(parent)
            public object NonCancellable : Job {}
            internal open class JobImpl(parent: Job?) : CompletableJob { }
            public fun Job(parent: Job? = null): CompletableJob = JobImpl(parent)
        """.trimIndent()
    )

    @Test
    fun `check detect SupervisorJob usage`() {
        testLintTask
            .files(
                LintDetectorTest.kotlin(
                    """
                        package ru.otus.homework.linthomework.jobinbuilderusage

                        import androidx.lifecycle.ViewModel
                        import androidx.lifecycle.viewModelScope
                        import kotlinx.coroutines.Dispatchers
                        import kotlinx.coroutines.Job
                        import kotlinx.coroutines.SupervisorJob
                        import kotlinx.coroutines.delay
                        import kotlinx.coroutines.launch

                        class JobInBuilderTestCase: ViewModel() {

                            fun case1() {
                                viewModelScope.launch(SupervisorJob() + Dispatchers.IO) {
                                    delay(1000)
                                    println("Hello World")
                                }
                            }
                        }
                    """.trimIndent()
                ),
                viewModelStub, viewModelExtensionsStub, coroutineStub
            )
            .run()
            .expectWarningCount(1)
            .expect(
                """
                    src/ru/otus/homework/linthomework/jobinbuilderusage/JobInBuilderTestCase.kt:14: Warning: Job/SupervisorJob нельзя передавать в корутин-билдер [JobInBuilderUsage]
        viewModelScope.launch(SupervisorJob() + Dispatchers.IO) {
                              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warnings
                """.trimIndent()
            )
    }

    @Test
    fun `check detect Job usage`() {
        testLintTask
            .files(
                LintDetectorTest.kotlin(
                    """
                        package ru.otus.homework.linthomework.jobinbuilderusage

                        import androidx.lifecycle.ViewModel
                        import androidx.lifecycle.viewModelScope
                        import kotlinx.coroutines.Job
                        import kotlinx.coroutines.delay
                        import kotlinx.coroutines.launch

                        class JobInBuilderTestCase : ViewModel() {

                            fun case2() {
                                viewModelScope.launch(Job()) {
                                    delay(1000)
                                    println("Hello World")
                                }
                            }
                        }
                    """.trimIndent()
                ),
                viewModelStub, viewModelExtensionsStub, coroutineStub
            )
            .run()
            .expectWarningCount(1)
            .expect(
                """
                    src/ru/otus/homework/linthomework/jobinbuilderusage/JobInBuilderTestCase.kt:12: Warning: Job/SupervisorJob нельзя передавать в корутин-билдер [JobInBuilderUsage]
                            viewModelScope.launch(Job()) {
                                                  ~~~~~
                    0 errors, 1 warnings
                """.trimIndent()
            )
    }

    @Test
    fun `check detect Job usage case3`() {
        testLintTask
            .files(
                LintDetectorTest.kotlin(
                    """
                        package ru.otus.homework.linthomework.jobinbuilderusage

                        import androidx.lifecycle.ViewModel
                        import androidx.lifecycle.viewModelScope
                        import kotlinx.coroutines.Job
                        import kotlinx.coroutines.delay
                        import kotlinx.coroutines.launch

                        class JobInBuilderTestCase(
                            private val job: Job
                        ) : ViewModel() {

                            fun case3() {
                                viewModelScope.launch(job) {
                                    delay(1000)
                                    println("Hello World")
                                }
                            }
                        }
                    """.trimIndent()
                ),
                viewModelStub, viewModelExtensionsStub, coroutineStub
            )
            .run()
            .expectWarningCount(1)
            .expect(
                """
                    src/ru/otus/homework/linthomework/jobinbuilderusage/JobInBuilderTestCase.kt:14: Warning: Job/SupervisorJob нельзя передавать в корутин-билдер [JobInBuilderUsage]
                            viewModelScope.launch(job) {
                                                  ~~~
                    0 errors, 1 warnings
                """.trimIndent()
            )
    }

    @Test
    fun `should detect Job usage case4`() {
        testLintTask
            .files(
                LintDetectorTest.kotlin(
                    """
                        package ru.otus.homework.linthomework.jobinbuilderusage

                        import androidx.lifecycle.ViewModel
                        import androidx.lifecycle.viewModelScope
                        import kotlinx.coroutines.Job
                        import kotlinx.coroutines.NonCancellable
                        import kotlinx.coroutines.delay
                        import kotlinx.coroutines.launch

                        class JobInBuilderTestCase: ViewModel() {

                            fun case4() {
                                viewModelScope.launch(NonCancellable) {
                                    delay(1000)
                                    println("Hello World")
                                }
                            }
                        }
                    """.trimIndent()
                ),
                viewModelStub, viewModelExtensionsStub, coroutineStub
            )
            .run()
            .expectWarningCount(1)
            .expect(
                """
                    src/ru/otus/homework/linthomework/jobinbuilderusage/JobInBuilderTestCase.kt:13: Warning: Job/SupervisorJob нельзя передавать в корутин-билдер [JobInBuilderUsage]
                            viewModelScope.launch(NonCancellable) {
                                                  ~~~~~~~~~~~~~~
                    0 errors, 1 warnings
                """.trimIndent()
            )
    }
}

