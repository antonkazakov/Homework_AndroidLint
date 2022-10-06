package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

internal class JobDetectorTest {

    @Test
    fun `should find globalScope usage`() {
        lint()
            .allowMissingSdk()
            .files(
                kotlin(
                    """
                        package checks

                        class TestClass {
                            fun onCreate() {
                                GlobalScope.launch(SupervisorJob() + SupervisorJob()) {
                                    delay(1000)
                                    println("Hello World")
                                }
                            }
                        }
                    """.trimIndent()
                ),
                kotlin(
                    """
                        package checks

                        import kotlinx.coroutines.CoroutineScope

                        object GlobalScope: CoroutineScope {
                            fun launch(job: Job, block: () -> Unit) {
                            }
                        }
                    """.trimIndent()
                ),
                kotlin(
                    """
                        package checks
                           
                        import kotlinx.coroutines.Job

                        class SupervisorJob: Job() {
                        }
                    """.trimIndent()
                ),
                kotlin(
                    """
                        package kotlinx.coroutines

                        class Job {
                            operator fun plus(job: Job): Job = job
                        }
                    """.trimIndent()
                ),
                kotlin(
                    """
                        package kotlinx.coroutines

                        class CoroutineScope {
                        }
                    """.trimIndent()
                )
            )
            .issues(JobDetector.ISSUE)
            .run()
            .expect(
                """
                    src/checks/TestClass.kt:3: Error: brief description [GlobalScopeUsage]
                    var scope = GlobalScope
                                ~~~~~~~~~~~
                    src/checks/TestClass.kt:7: Error: brief description [GlobalScopeUsage]
                            GlobalScope.launch{ }
                            ~~~~~~~~~~~
                    2 errors, 0 warnings
                """.trimIndent()
            )
    }

//    @Test
//    fun `second`() {
//        lint()
//            .allowMissingSdk()
//            .files(
//                kotlin(
//                    """
//                        package checks
//
//                        class TestClass {
//                            fun onCreate() {
//                                GlobalScope.launch {
//                                    launch(NonCancellable()) {
//                                        delay(1000)
//                                        println("Hello World")
//                                    }
//                                }
//                            }
//                        }
//                    """.trimIndent()
//                ),
//                kotlin(
//                    """
//                        package checks
//
//                        import kotlinx.coroutines.CoroutineScope
//                        import kotlinx.coroutines.NonCancelable
//
//                        object GlobalScope: CoroutineScope {
//                            override fun launch(job: Job, block: CoroutineScope.() -> Unit) {
//                            }
//                        }
//                    """.trimIndent()
//                ),
//                kotlin(
//                    """
//                        package kotlinx.coroutines
//
//                        class NonCancelable: Job() {
//                        }
//                    """.trimIndent()
//                ),
//                kotlin(
//                    """
//                        package kotlinx.coroutines
//
//                        class Job {
//                        }
//                    """.trimIndent()
//                ),
//                kotlin(
//                    """
//                        package kotlinx.coroutines
//
//                        class CoroutineScope {
//                             fun launch(job: Job, block: CoroutineScope.() -> Unit) {
//                             }
//                        }
//                    """.trimIndent()
//                )
//            )
//            .issues(JobDetector.ISSUE)
//            .run()
//            .expect(
//                """
//                    src/checks/TestClass.kt:3: Error: brief description [GlobalScopeUsage]
//                    var scope = GlobalScope
//                                ~~~~~~~~~~~
//                    src/checks/TestClass.kt:7: Error: brief description [GlobalScopeUsage]
//                            GlobalScope.launch{ }
//                            ~~~~~~~~~~~
//                    2 errors, 0 warnings
//                """.trimIndent()
//            )
//    }

}