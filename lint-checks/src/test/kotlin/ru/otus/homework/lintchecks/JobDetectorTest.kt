package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin

class JobDetectorTest {

    @Test
    fun `Testing coroutine builders`() {
        lint()
            .allowMissingSdk()
            .files(
                kotlin(
                    """
                        package kotlinx.coroutines

                        class CoroutineScope {}
                        class Job {}
                        class SupervisorJob: Job() {}
                        object GlobalScope: CoroutineScope {
                            fun launch(job: Job, block: () -> Unit) {}
                        }
                    """.trimIndent()
                ),
                kotlin(
                    """
                        package tests
                           
                        import kotlinx.coroutines.*

                        class ViewModelTest {
                            init {
                                GlobalScope.launch(SupervisorJob()) {
                                    delay(1000)
                                    println("print ViewModelTest")
                                }
                            }
                        }
                    """.trimIndent()
                ),

                )
            .issues(JobDetector.ISSUE)
            .run()
            .expect(
                """
                    src/tests/ViewModelTest.kt:7: Error: Job / Supervisor Job isn`t allowed [JobInBuilderUsage]
                            GlobalScope.launch(SupervisorJob()) {
                                               ~~~~~~~~~~~~~~~
                    1 errors, 0 warnings
                """.trimIndent()
            )
    }
}