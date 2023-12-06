package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test
import ru.otus.homework.lintchecks.CommonStubs.channelsStub
import ru.otus.homework.lintchecks.CommonStubs.coroutinesStub
import ru.otus.homework.lintchecks.CommonStubs.viewModelStub

class GlobalScopeDetectorTest {

    private val lintTask = TestLintTask.lint()
        .allowMissingSdk()
        .issues(GlobalScopeDetector.ISSUE)

    @Test
    fun `check global scope usage case 1`() {
        val file = LintDetectorTest.kotlin(
            """
                import androidx.lifecycle.ViewModel
                import kotlinx.coroutines.GlobalScope
                import kotlinx.coroutines.delay
                import kotlinx.coroutines.launch
                import kotlinx.coroutines.channels.actor
                
                class GlobalScopeTestCase : ViewModel() {
                
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
        val expected =
            """
                src/GlobalScopeTestCase.kt:10: Warning: Не используйте GlobalScope [GlobalScopeUsage]
                        GlobalScope.launch {
                        ^
                src/GlobalScopeTestCase.kt:14: Warning: Не используйте GlobalScope [GlobalScopeUsage]
                        GlobalScope.actor<String> {
                        ^
                0 errors, 2 warnings
            """.trimIndent()
        check(file, expected)
    }

    @Test
    fun `check global scope usage case 2`() {
        val file = LintDetectorTest.kotlin(
            """
                import androidx.lifecycle.ViewModel
                import androidx.lifecycle.viewModelScope
                import kotlinx.coroutines.CoroutineScope
                import kotlinx.coroutines.GlobalScope
                import kotlinx.coroutines.async
                import kotlinx.coroutines.delay
                import kotlinx.coroutines.launch
                
                class GlobalScopeTestCase : ViewModel() {
                
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
        val expected =
            """
               src/GlobalScopeTestCase.kt:13: Warning: Не используйте GlobalScope [GlobalScopeUsage]
                           val deferred = GlobalScope.async {
                                          ^
               0 errors, 1 warnings
            """.trimIndent()
        check(file, expected)
    }

    @Test
    fun `check global scope usage case 3`() {
        val file = LintDetectorTest.kotlin(
            """
                import androidx.lifecycle.ViewModel
                import kotlinx.coroutines.CoroutineScope
                import kotlinx.coroutines.delay
                import kotlinx.coroutines.launch
                
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
        val expected = """No warnings.""".trimIndent()
        lintTask
            .files(file, coroutinesStub, viewModelStub)
            .run()
            .expectWarningCount(0)
            .expect(expected)
    }

    @Test
    fun `check global scope launch usage`() {
        val file = LintDetectorTest.kotlin(
            """
                import kotlinx.coroutines.GlobalScope
                import kotlinx.coroutines.launch
                
                class GlobalScopeTestCase {
                
                    fun callGlobalScope() {
                        GlobalScope.launch {}
                    }
                }
            """.trimIndent()
        )
        val expected =
            """
                src/GlobalScopeTestCase.kt:7: Warning: Не используйте GlobalScope [GlobalScopeUsage]
                        GlobalScope.launch {}
                        ~~~~~~~~~~~~~~~~~~~~~
                0 errors, 1 warnings
            """.trimIndent()
        check(file, expected)
    }

    @Test
    fun `check global scope async usage`() {
        val file = LintDetectorTest.kotlin(
            """
                import kotlinx.coroutines.GlobalScope
                import kotlinx.coroutines.async
                
                class GlobalScopeTestCase {
                
                    fun callGlobalScope() {
                        GlobalScope.async {}
                    }
                }
            """.trimIndent()
        )
        val expected =
            """
                src/GlobalScopeTestCase.kt:7: Warning: Не используйте GlobalScope [GlobalScopeUsage]
                        GlobalScope.async {}
                        ~~~~~~~~~~~~~~~~~~~~
                0 errors, 1 warnings
            """.trimIndent()
        check(file, expected)
    }

    @Test
    fun `check global scope runBlocking usage`() {
        val file = LintDetectorTest.kotlin(
            """
                import kotlinx.coroutines.GlobalScope
                import kotlinx.coroutines.runBlocking
                
                class GlobalScopeTestCase {
                
                    fun callGlobalScope() {
                        GlobalScope.runBlocking {}
                    }
                }
            """.trimIndent()
        )
        val expected =
            """
                src/GlobalScopeTestCase.kt:7: Warning: Не используйте GlobalScope [GlobalScopeUsage]
                        GlobalScope.runBlocking {}
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 1 warnings
            """.trimIndent()
        check(file, expected)
    }

    @Test
    fun `check global scope usage with assignment`() {
        val file = LintDetectorTest.kotlin(
            """
                import kotlinx.coroutines.GlobalScope
                import kotlinx.coroutines.launch
                
                class GlobalScopeTestCase {
                
                    fun callGlobalScope() {
                        val job = GlobalScope.launch {}
                    }
                }
            """.trimIndent()
        )
        val expected =
            """
                src/GlobalScopeTestCase.kt:7: Warning: Не используйте GlobalScope [GlobalScopeUsage]
                        val job = GlobalScope.launch {}
                                  ~~~~~~~~~~~~~~~~~~~~~
                0 errors, 1 warnings
            """.trimIndent()
        check(file, expected)
    }

    @Test
    fun `check global scope usage as field`() {
        val file = LintDetectorTest.kotlin(
            """
                import kotlinx.coroutines.GlobalScope
                import kotlinx.coroutines.launch
                
                private val scope = GlobalScope
                
                class GlobalScopeTestCase {
                
                    fun callGlobalScope() {
                        scope.launch {}
                    }
                }
            """.trimIndent()
        )
        val expected =
            """
                src/GlobalScopeTestCase.kt:9: Warning: Не используйте GlobalScope [GlobalScopeUsage]
                        scope.launch {}
                        ~~~~~~~~~~~~~~~
                0 errors, 1 warnings
            """.trimIndent()
        check(file, expected)
    }

    private fun check(file: TestFile, expected: String) {
        lintTask.files(file, coroutinesStub, channelsStub, viewModelStub)
            .run()
            .expect(expected)
    }
}
