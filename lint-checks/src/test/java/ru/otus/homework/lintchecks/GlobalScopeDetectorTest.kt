package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

@Suppress("UnstableApiUsage")
internal class GlobalScopeDetectorTest {

    private val lintTask = lint().allowMissingSdk().issues(GlobalScopeDetector.ISSUE)

    @Test
    fun `should find globalScope usage`() {
        lintTask
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
            .run()
            .expect(
                """
                    src/checks/TestClass.kt:2: Warning: Замените 'GlobalScope' на другой вид 'CoroutineScope' [GlobalScopeUsage]
                    var scope = GlobalScope
                                ~~~~~~~~~~~
                    src/checks/TestClass.kt:5: Warning: Замените 'GlobalScope' на другой вид 'CoroutineScope' [GlobalScopeUsage]
                            GlobalScope.launch{ }
                            ~~~~~~~~~~~
                    0 errors, 2 warnings
                """.trimIndent()
            )
    }

    @Test
    fun `should find GlobalScope in ViewModel`() {
        lintTask
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
            .run()
            .expectWarningCount(1)
    }

    @Test
    fun `should find 3 GlobalScope usage`() {
        lintTask
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
            .run()
            .expectWarningCount(3)
    }
}
