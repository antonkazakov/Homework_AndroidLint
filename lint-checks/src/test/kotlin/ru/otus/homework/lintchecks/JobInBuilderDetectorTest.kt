package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

internal class JobInBuilderDetectorTest {

    @Test
    fun `Check for jobs in coroutine builders`() {
        lint()
            .allowMissingSdk()
            .files(
                kotlin(
                    """
                        package kotlinx.coroutines

                        class CoroutineScope { }
                        class Job { }
                        class SupervisorJob: Job() {  }

                        object GlobalScope: CoroutineScope {
                            fun launch(job: Job, block: () -> Unit) { }
                        }
                    """.trimIndent()
                ),
                kotlin(
                    """
                        package tests
                           
                        import kotlinx.coroutines.*

                        class MyViewModel {
                            init {
                                GlobalScope.launch(SupervisorJob()) {
                                    delay(1000)
                                    println("Hello World")
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
                    src/tests/MyViewModel.kt:7: Error: Job / Supervisor Job is not allowed [JobInBuilderUsage]
                            GlobalScope.launch(SupervisorJob()) {
                                               ~~~~~~~~~~~~~~~
                    1 errors, 0 warnings
                """.trimIndent()
            )
    }

}