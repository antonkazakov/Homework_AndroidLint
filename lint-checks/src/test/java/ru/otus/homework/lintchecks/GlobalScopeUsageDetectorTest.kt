package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

@Suppress("UnstableApiUsage")
internal class GlobalScopeUsageDetectorTest {
    private val lintTask = lint().allowMissingSdk().issues(GlobalScopeUsageDetector.ISSUE)

    @Test
    fun testGlobalScopeInCase1() {
        lintTask
            .files(
                kotlin("""
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
            """)
            )
            .run()
            .expect("""
            src/GlobalScopeTestCase.kt:5: Warning: Avoid using GlobalScope for coroutines [GlobalScopeUsage]
                                    GlobalScope.launch {
                                    ~~~~~~~~~~~
            src/GlobalScopeTestCase.kt:9: Warning: Avoid using GlobalScope for coroutines [GlobalScopeUsage]
                                    GlobalScope.actor<String> {
                                    ~~~~~~~~~~~
            0 errors, 2 warnings
        """)
    }

    @Test
    fun testGlobalScopeInCase2() {
        lintTask
            .files(
                kotlin("""
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
            """)
            )
            .run()
            .expect("""
            src/GlobalScopeTestCase.kt:6: Warning: Avoid using GlobalScope for coroutines [GlobalScopeUsage]
                                        val deferred = GlobalScope.async {
                                                       ~~~~~~~~~~~
            0 errors, 1 warnings
        """)
    }

    @Test
    fun testNoWarningInCase3() {
        lintTask
            .files(
                kotlin("""
                class GlobalScopeTestCase(private val scope: CoroutineScope) : ViewModel() {
                    
                    fun case3() {
                        scope.launch {
                            delay(1000)
                            println("Hello World")
                        }
                    }
                }
            """)
            )
            .run()
            .expectClean()
    }

    @Test
    fun should_find_GlobalScope_in_ViewModel() {
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
}