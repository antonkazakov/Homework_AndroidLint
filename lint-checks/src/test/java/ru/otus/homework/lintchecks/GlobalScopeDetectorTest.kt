package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test

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
            .files(file, globalScopeStub, viewModelStub)
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
        lintTask.files(file, globalScopeStub, channelsStub, viewModelStub)
            .run()
            .expect(expected)
    }

    private val globalScopeStub = LintDetectorTest.kotlin(
        """
            package kotlinx.coroutines
            
            interface CoroutineScope
            object GlobalScope : CoroutineScope
            
            fun CoroutineScope.launch(block: suspend () -> Unit) {}
            fun CoroutineScope.async(block: suspend () -> Unit) {}
            fun CoroutineScope.runBlocking(block: suspend () -> Unit) {}
            fun delay(timeMillis: Long) {}
        """.trimIndent()
    )

    private val channelsStub = LintDetectorTest.kotlin(
        """
            package kotlinx.coroutines.channels
            
            import kotlinx.coroutines.*
            
            fun <E> CoroutineScope.actor(block: suspend () -> Unit)
        """.trimIndent()
    )
    private val viewModelStub = LintDetectorTest.kotlin(
        """
            package androidx.lifecycle
            
            import kotlinx.coroutines.CoroutineScope
            
            abstract class ViewModel    
            val ViewModel.viewModelScope: CoroutineScope
        """.trimIndent()
   )

    /***
     *  Все тесты ниже падают с ошибкой AssertionError
     *  Не смог разобраться, что делаю не так
     */

    @Test
    fun `check replace with view model scope using build gradle`() {
        val file = LintDetectorTest.kotlin(
            """
                import kotlinx.coroutines.GlobalScope
                
                class GlobalScopeTestCase {
                
                    fun callGlobalScope() {
                        GlobalScope.launch {}
                    }
                }
            """.trimIndent()
        )
        val expected =
            """
                Fix for src/GlobalScopeTestCase.kt line 6: Заменить GlobalScope на viewModelScope:
                @@ -8 +8
                -         GlobalScope.launch {
                +         viewModelScope.launch {
            """.trimIndent()
        checkWithGradle(file, buildGradleStub, expected)
    }

    @Test
    fun `check replace with view model scope using build gradle kts`() {
        val file = LintDetectorTest.kotlin(
            """
                import kotlinx.coroutines.GlobalScope
                
                class GlobalScopeTestCase {
                
                    fun callGlobalScope() {
                        GlobalScope.launch {}
                    }
                }
            """.trimIndent()
        )
        val expected =
            """
                Fix for src/GlobalScopeTestCase.kt line 6: Заменить GlobalScope на viewModelScope:
                @@ -8 +8
                -         GlobalScope.launch {
                +         viewModelScope.launch {
            """.trimIndent()
        checkWithGradle(file, buildGradleKtsStub, expected)
    }

    @Test
    fun `check replace with lifecycle scope using build gradle`() {
        val file = LintDetectorTest.kotlin(
            """
                import kotlinx.coroutines.GlobalScope
                
                class GlobalScopeTestCase {
                
                    fun callGlobalScope() {
                        GlobalScope.launch {}
                    }
                }
            """.trimIndent()
        )
        val expected =
            """
                Fix for src/GlobalScopeTestCase.kt line 6: Заменить GlobalScope на lifecycleScope:
                @@ -8 +8
                -         GlobalScope.launch {
                +         lifecycleScope.launch {
            """.trimIndent()
        checkWithGradle(file, buildGradleStub, expected)
    }

    @Test
    fun `check replace with lifecycle scope using build gradle kts`() {
        val file = LintDetectorTest.kotlin(
            """
                import kotlinx.coroutines.GlobalScope
                
                class GlobalScopeTestCase {
                
                    fun callGlobalScope() {
                        GlobalScope.launch {}
                    }
                }
            """.trimIndent()
        )
        val expected =
            """
                Fix for src/GlobalScopeTestCase.kt line 6: Заменить GlobalScope на lifecycleScope:
                @@ -8 +8
                -         GlobalScope.launch {
                +         lifecycleScope.launch {
            """.trimIndent()
        checkWithGradle(file, buildGradleKtsStub, expected)
    }

    private fun checkWithGradle(file: TestFile, gradleFile: TestFile, expected: String) {
        lintTask.files(file, globalScopeStub, gradleFile)
            .run()
            .expectFixDiffs(expected)
    }

    private val buildGradleStub = LintDetectorTest.gradle(
        """
            dependencies {
                implementation "androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2"
                implementation "androidx.lifecycle:lifecycle-runtime-ktx:2.6.2"
            }
        """.trimIndent()
    )

    private val buildGradleKtsStub = LintDetectorTest.kts(
        """
            dependencies {
                implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
                implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
            }
        """.trimIndent()
    )
}
