package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

internal class JobDetectorTest {

    @Test
    fun `should find job in launch builder`() {
        lint()
            .allowMissingSdk()
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
            .issues(JobDetector.ISSUE)
            .run()
            .expect(
                """
                    src/checks/TestClass.kt:7: Error: brief description [JobInBuilderUsage]
                            GlobalScope.launch(SupervisorJob()) {
                                               ~~~~~~~~~~~~~~~
                    1 errors, 0 warnings
                """.trimIndent()
            )
    }

}