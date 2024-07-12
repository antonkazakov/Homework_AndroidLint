package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test

@Suppress("UnstableApiUsage")
class GlobalScopeDetectorTest {

    private val detector = GlobalScopeDetector()

    @Test
    fun `detect GlobalScope usage`() {
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
            .issues(GlobalScopeDetector.ISSUE)
            .run()
            .expectWarningCount(2)
    }

    @Test
    fun `detect no warning if there is no GlobalScope usage`() {
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
            .issues(GlobalScopeDetector.ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun `detect usage into ViewModel Scope`() {
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
            .issues(GlobalScopeDetector.ISSUE)
            .run()
            .expectWarningCount(1)
    }

    private fun lint() = TestLintTask.lint().apply {
        allowMissingSdk()
        issues(GlobalScopeDetector.ISSUE)
        detector(detector)
    }
}
