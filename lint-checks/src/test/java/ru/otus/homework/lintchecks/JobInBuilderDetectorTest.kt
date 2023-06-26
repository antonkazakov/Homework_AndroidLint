package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test

@Suppress("UnstableApiUsage")
internal class JobInBuilderDetectorTest {

    private val lintTask = lint().allowMissingSdk().issues(JobInBuilderDetector.ISSUE)

    @Test
    fun should_detect_Job() {
        lintTask.files(
            kotlin(
                """
                package ru.otus.homework.linthomework.jobinbuilderusage
                import androidx.lifecycle.ViewModel
                import androidx.lifecycle.viewModelScope
                import kotlinx.coroutines.Dispatchers
                import kotlinx.coroutines.Job
                import kotlinx.coroutines.NonCancellable
                import kotlinx.coroutines.SupervisorJob
                import kotlinx.coroutines.launch
                class JobInBuilderTestCase(
                    private val job: Job
                ) : ViewModel() {
                    fun case1() {
                        viewModelScope.launch(SupervisorJob() + Dispatchers.IO) {
                            println("Hello World")
                        }
                    }
                    fun case2() {
                        viewModelScope.launch(Job()) {
                            println("Hello World")
                        }
                    }
                    fun case3() {
                        viewModelScope.launch(job) {
                            println("Hello World")
                        }
                    }
                    fun case4() {
                        viewModelScope.launch(NonCancellable) {
                            println("Hello World")
                        }
                    }
                    fun case5() {
                        viewModelScope.launch(SupervisorJob()) {
                            println("Hello World")
                        }
                    }
                }
            """.trimIndent()
            ),
            kotlinxCoroutines,
            kotlinCoroutines,
            androidxLifecycle
        )
            .testModes(TestMode.DEFAULT)
            .run()
            .expect(
                """
                src/ru/otus/homework/linthomework/jobinbuilderusage/JobInBuilderTestCase.kt:13: Warning: Job or SupervisorJob use in coroutine builder is not allowed. [JobInBuilderUsage]
                        viewModelScope.launch(SupervisorJob() + Dispatchers.IO) {
                                              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/ru/otus/homework/linthomework/jobinbuilderusage/JobInBuilderTestCase.kt:18: Warning: Job or SupervisorJob use in coroutine builder is not allowed. [JobInBuilderUsage]
                        viewModelScope.launch(Job()) {
                                              ~~~~~
                src/ru/otus/homework/linthomework/jobinbuilderusage/JobInBuilderTestCase.kt:23: Warning: Job or SupervisorJob use in coroutine builder is not allowed. [JobInBuilderUsage]
                        viewModelScope.launch(job) {
                                              ~~~
                src/ru/otus/homework/linthomework/jobinbuilderusage/JobInBuilderTestCase.kt:28: Warning: Job or SupervisorJob use in coroutine builder is not allowed. [JobInBuilderUsage]
                        viewModelScope.launch(NonCancellable) {
                                              ~~~~~~~~~~~~~~
                src/ru/otus/homework/linthomework/jobinbuilderusage/JobInBuilderTestCase.kt:33: Warning: Job or SupervisorJob use in coroutine builder is not allowed. [JobInBuilderUsage]
                        viewModelScope.launch(SupervisorJob()) {
                                              ~~~~~~~~~~~~~~~
                0 errors, 5 warnings
            """.trimIndent()
            )
    }

    @Test
    fun should_find_job_in_launch_builder() {
       lintTask
            .files(
                kotlin(
                    """
                        package checks
                           
                        import kotlinx.coroutines.*
                        class TestClass {
                            fun onCreate() {
                                GlobalScope.launch(SupervisorJob()) {
                                    delay(1000)
                                    println("Hello World")
                                }
                            }
                        }
                    """.trimIndent()
                ),
                kotlin(
                    """
                        package kotlinx.coroutines
                        object GlobalScope: CoroutineScope {
                            fun launch(job: Job, block: () -> Unit) { }
                        }
                    """.trimIndent()
                ),
                kotlin(
                    """
                        package kotlinx.coroutines
                        class SupervisorJob: Job() {  }
                    """.trimIndent()
                ),
                kotlin(
                    """
                        package kotlinx.coroutines
                        class Job { }
                    """.trimIndent()
                ),
                kotlin(
                    """
                        package kotlinx.coroutines
                        class CoroutineScope { }
                    """.trimIndent()
                )
            )
            .run()
            .expect(
                """
                    src/checks/TestClass.kt:6: Warning: Job or SupervisorJob use in coroutine builder is not allowed. [JobInBuilderUsage]
                            GlobalScope.launch(SupervisorJob()) {
                                               ~~~~~~~~~~~~~~~
                    0 errors, 1 warnings
                """.trimIndent()
            )
    }
}