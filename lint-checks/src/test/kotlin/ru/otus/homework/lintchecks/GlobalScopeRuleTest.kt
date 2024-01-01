package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test

@Suppress("UnstableApiUsage")
class GlobalScopeRuleTest {

    @Test
    fun `detekt usage`() {
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
            .issues(GlobalScopeRule.ISSUE)
            .run()
            .expectWarningCount(2)
    }

    @Test
    fun `detect no warning if it is no usage`() {
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
            .issues(GlobalScopeRule.ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun `detekt usage into ViewModel Scope`() {
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
            .issues(GlobalScopeRule.ISSUE)
            .run()
            .expectWarningCount(1)
    }
}
