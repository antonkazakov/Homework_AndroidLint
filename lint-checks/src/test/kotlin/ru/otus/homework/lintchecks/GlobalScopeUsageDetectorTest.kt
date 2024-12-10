package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

@Suppress("UnstableApiUsage")
internal class GlobalScopeUsageDetectorTest {
    private val lintTask: TestLintTask = lint().allowMissingSdk().issues(GlobalScopeUsageDetector.ISSUE)


    @Test
    fun testGlobalScopeCase0() {
        lintTask
            .files(
                kotlin(
                    """
                    class GlobalScopeTestCase(private val scope: CoroutineScope) : ViewModel() {
                    
                        fun case0() {
                            GlobalScope.launch {
                                delay(1000)
                                println("Hello World")
                            }
                        }
                    }
               """
                )
            )
            .run()
            .expect(
                """src/GlobalScopeTestCase.kt:5: Warning: Don't use GlobalScope for coroutines [GlobalScopeUsage]
                            GlobalScope.launch {
                            ~~~~~~~~~~~
0 errors, 1 warnings                 
            """.trimIndent()
            )
    }

    @Test
    fun testGlobalScopeCase1() {
        lintTask
            .files(
                kotlin(
                    """
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
               """
                )
            )
            .run()
            .expect(
                """src/GlobalScopeTestCase.kt:5: Warning: Don't use GlobalScope for coroutines [GlobalScopeUsage]
                                GlobalScope.launch {
                                ~~~~~~~~~~~
src/GlobalScopeTestCase.kt:9: Warning: Don't use GlobalScope for coroutines [GlobalScopeUsage]
                                GlobalScope.actor<String> {
                                ~~~~~~~~~~~
0 errors, 2 warnings""".trimIndent()
            )
    }

    @Test
    fun testGlobalScopeCase2() {
        lintTask
            .files(
                kotlin(
                    """
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
               """
                )
            )
            .run()
            .expect(
                """src/GlobalScopeTestCase.kt:5: Warning: Don't use GlobalScope for coroutines [GlobalScopeUsage]
                                val deferred = GlobalScope.async {
                                               ~~~~~~~~~~~
0 errors, 1 warnings""".trimIndent()
            )
    }

    @Test
    fun testGlobalScopeCase3() {
        lintTask
            .files(
                kotlin(
                    """
                    class GlobalScopeTestCase(private val scope: CoroutineScope) : ViewModel() {
                           fun case3() {
                                scope.launch {
                                    delay(1000)
                                    println("Hello World")
                                }
                            }
                    }
               """
                )
            )
            .run()
            .expectClean()
    }
}