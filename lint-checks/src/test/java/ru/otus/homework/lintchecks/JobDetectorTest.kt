package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

@Suppress("UnstableApiUsage")
internal class JobDetectorTest {

    @Test
    fun `check SupervisorJob in launch builder`() {
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
                        class CoroutineScope { }
                        class Job { }
                        class SupervisorJob: Job() {  }
                        object GlobalScope: CoroutineScope {
                            fun launch(job: Job, block: () -> Unit) { }
                        }
                    """.trimIndent()
                )
            )
            .issues(JobDetector.ISSUE)
            .run()
            .expect(
                """
                    src/checks/TestClass.kt:6: Error: Job / Supervisor Job не допускается в корутин-билдере [JobInBuilderUsage]
                            GlobalScope.launch(SupervisorJob()) {
                                               ~~~~~~~~~~~~~~~
                    1 errors, 0 warnings
                """.trimIndent()
            )
    }

}