package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

internal class GlobalScopeDetectorTest {

    @Test
    fun `should find globalScope usage`() {
        lint()
            .allowMissingSdk()
            .files(
                kotlin(
                    """
                        package checks

                        var scope = GlobalScope

                        class TestClass {
                            fun onCreate() {
                                GlobalScope.launch{ }
                            }
                        }
                    """.trimIndent()
                )
            )
            .issues(GlobalScopeDetector.ISSUE)
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

    @Test
    fun `should find GlobalScope in ViewModel`() {
        lint()
            .allowMissingSdk()
            .files(
                kotlin(
                    """
                        package checks

                        class TestViewModel : TestParentViewModel() {
                            fun onCreate() {
                                GlobalScope.launch{ }
                            }
                        }
                    """.trimIndent()
                ),
                kotlin(
                    """
                        package checks
    
                        import androidx.lifecycle.ViewModel

                        class TestParentViewModel : ViewModel() {

                        }
                    """.trimIndent()
                ),
                kotlin(
                    """
                        package androidx.lifecycle

                        class ViewModel {

                        }
                    """.trimIndent()
                )
            )
            .issues(GlobalScopeDetector.ISSUE)
            .run()
            .expectErrorCount(1)
    }

    @Test
    fun `should find 3 GlobalScope usage`() {
        lint()
            .allowMissingSdk()
            .files(
                kotlin(
                    """
                        @file:OptIn(DelicateCoroutinesApi::class)

                        package ru.otus.homework.linthomework.globalscopeusage

                        class GlobalScopeTestCase(private val scope: CoroutineScope) : ViewModel() {

                            fun case1() {
                                GlobalScope.launch {
                                    delay(1000)
                                    println("Hello World")
                                }
                                GlobalScope.actor<String> {
                                    delay(1000)
                                    println("Hello World")
                                }
                            }

                            fun case2() {
                                viewModelScope.launch {
                                    val deferred = GlobalScope.async {
                                        delay(1000)
                                        "Hello World"
                                    }
                                    println(deferred.await())
                                }
                            }

                            fun case3() {
                                scope.launch {
                                    delay(1000)
                                    println("Hello World")
                                }
                            }
                        }
                    """.trimIndent()
                )
            )
            .issues(GlobalScopeDetector.ISSUE)
            .run()
            .expectErrorCount(3)
    }

}