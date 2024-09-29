package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

class GlobalScopeUsageDetectorTest {

    @Test
    fun `test detection of GlobalScope usage (case1)`() {
        lint().files(
            LintDetectorTest.kotlin(
                """
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
                }
                """.trimIndent()
            )
        )
            .issues(GlobalScopeUsageDetector.ISSUE)
            .run()
            .expectWarningCount(2)
    }

    @Test
    fun `test detection of GlobalScope usage within ViewModel Scope (case2)`() {
        lint().files(
            LintDetectorTest.kotlin(
                """
                package ru.otus.homework.linthomework.globalscopeusage
                
                class GlobalScopeTestCase(private val scope: CoroutineScope) : ViewModel() {
                
                    fun case2() {
                        viewModelScope.launch {
                            val deferred = GlobalScope.async {
                                delay(1000)
                                "Hello World"
                            }
                            println(deferred.await())
                        }
                    }
                }
                """.trimIndent()
            )
        )
            .issues(GlobalScopeUsageDetector.ISSUE)
            .run()
            .expectWarningCount(1)
    }

    @Test
    fun `test no detection of GlobalScope usage (case3)`() {
        lint().files(
            LintDetectorTest.kotlin(
                """
                package ru.otus.homework.linthomework.globalscopeusage
                
                class GlobalScopeTestCase(private val scope: CoroutineScope) : ViewModel() {
                
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
            .issues(GlobalScopeUsageDetector.ISSUE)
            .run()
            .expectClean()
    }
}