package ru.otus.homework.lintchecks.rules

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test

@Suppress("UnstableApiUsage")
class GlobalScopeUsageRuleTest {

    @Test
    fun detektDoubleUsageGlobalScope() {
        TestLintTask.lint()
            .allowMissingSdk()
            .files(
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
            .issues(GlobalScopeUsageRule.ISSUE)
            .run()
            .expectWarningCount(2)
    }

    @Test
    fun detektUsageGlobalScopeIntoViewModelScope() {
        TestLintTask.lint()
            .allowMissingSdk()
            .files(
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
            .issues(GlobalScopeUsageRule.ISSUE)
            .run()
            .expectWarningCount(1)
    }

    @Test
    fun noWarningWhenNeverUsageGlobalScope() {
        TestLintTask.lint()
            .allowMissingSdk()
            .files(
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
            .issues(GlobalScopeUsageRule.ISSUE)
            .run()
            .expectClean()
    }
}