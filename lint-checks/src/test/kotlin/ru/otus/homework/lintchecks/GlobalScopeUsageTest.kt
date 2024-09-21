package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test
import ru.otus.homework.lintchecks.globalscope.GlobalScopeDetector
import ru.otus.homework.lintchecks.globalscope.GlobalScopeIssue

@Suppress("UnstableApiUsage")
class GlobalScopeUsageTest {

    @Test
    fun show_warnings_if_GlobalScope_used() {
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
            .detector(GlobalScopeDetector())
            .issues(GlobalScopeIssue.ISSUE)
            .run()
            .expect(
                """
                    src/checks/TestClass.kt:3: Warning: Try to avoid GlobalScope [GlobalScopeIssue]
                    var scope = GlobalScope
                                ~~~~~~~~~~~
                    src/checks/TestClass.kt:6: Warning: Try to avoid GlobalScope [GlobalScopeIssue]
                            GlobalScope.launch{ }
                            ~~~~~~~~~~~
                    0 errors, 2 warnings
                """.trimIndent()
            )
    }

    @Test
    fun show_numbers_of_GlobalScope_warnings() {
        lint()
            .allowMissingSdk()
            .files(
                kotlin(
                    """
                        @file:OptIn(DelicateCoroutinesApi::class)
                        package ru.tyryshkin.runtime.lint.test              
                        
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
            .issues(GlobalScopeIssue.ISSUE)
            .run()
            .expectErrorCount(0)
            .expectWarningCount(3)
    }
}
