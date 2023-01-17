package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Assert.*
import org.junit.Test

@Suppress("UnstableApiUsage")
class GlobalScopeDetectorTest {

    private val lintTask = TestLintTask.lint().allowMissingSdk().issues(GlobalScopeDetector.ISSUE)

    @Test
    fun `should detect GlobalScope usage`() {
        lintTask.files(
            LintDetectorTest.kotlin(
                """
                        package ru.otus.homework.linthomework.globalscopeusage

                        class GlobalScopeTestCase(private val scope: CoroutineScope) : ViewModel() {
                        
                            fun case1() {
                                GlobalScope.launch {
                                    delay(1000)
                                    println("Hello World")
                                }
                                viewModelScope.actor<String> {
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

}
                    """.trimIndent()
            )
        )
            .run()
            .expect("""
                src/ru/otus/homework/linthomework/globalscopeusage/GlobalScopeTestCase.kt:6: Warning: GlobalScope should be used with caution. [GlobalScopeUsage]
                                GlobalScope.launch {
                                ~~~~~~~~~~~
src/ru/otus/homework/linthomework/globalscopeusage/GlobalScopeTestCase.kt:17: Warning: GlobalScope should be used with caution. [GlobalScopeUsage]
                                    val deferred = GlobalScope.async {
                                                   ~~~~~~~~~~~
0 errors, 2 warnings
""".trimIndent())
    }

    @Test
    fun `should not detect GlobalScope usage`() {
        lintTask.files(
            LintDetectorTest.kotlin(
                """
                        package ru.otus.homework.linthomework.globalscopeusage
                        
                        class GlobalScopeTestCase(private val scope: CoroutineScope) : ViewModel() {
                        
                            fun case1() {
                                viewmodelScope.launch {
                                    delay(1000)
                                    println("Hello World")
                                }
                                viewmodelScope.actor<String> {
                                    delay(1000)
                                    println("Hello World")
                                }
                            }
}
                    """.trimIndent()
            )
        )
            .run()
            .expect("""
                No warnings.
            """.trimIndent())
    }
}