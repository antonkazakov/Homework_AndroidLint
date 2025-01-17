package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

@Suppress("UnstableApiUsage")
internal class GlobalScopeDetectorTest {

    private val lintTask = lint().allowMissingSdk().issues(GlobalScopeUsage.ISSUE)

    @Test
    fun globalScopeTestCase() {
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
