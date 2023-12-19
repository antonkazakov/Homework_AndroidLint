package ru.otus.homework.lintchecks

import org.junit.Test
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask

class JobInBuilderDetectorTest {

    private val lintTask = TestLintTask
        .lint()
        .allowMissingSdk()
        .issues(JobInBuilderDetector.ISSUE)

    @Test
    fun `Testing coroutine builders`() {
        lintTask
            .files(
                kotlin(
                    """
                        package kotlinx.coroutines
                        class Job {}
                        class CoroutineScope {}
                        class SupervisorJob: Job() {}
                        object GlobalScope: CoroutineScope {
                            fun launch(job: Job, block: () -> Unit) {}
                        }
                    """.trimIndent()
                ),
                kotlin(
                    """
                        package test
                        import kotlinx.coroutines.*
                        class ViewModelTest {
                            init {
                                GlobalScope.launch(SupervisorJob()) {
                                    println("ViewModelTest")
                                    delay(100)
                                }
                            }
                        }
                    """.trimIndent()
                ),

                )
            .run()
            .expect(
                """
                    src/test/ViewModelTest.kt:5: Warning: Job / Supervisor Job isn`t allowed [JobInBuilderUsage]
                            GlobalScope.launch(SupervisorJob()) {
                                               ~~~~~~~~~~~~~~~
                    0 errors, 1 warnings
                """.trimIndent()
            )
    }
}
