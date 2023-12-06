package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test
import ru.otus.homework.lintchecks.GlobalScopeDetector

class GlobalScopeDetectorTest {

    private val lintTask = TestLintTask.lint()
        .allowMissingSdk()
        .issues(GlobalScopeDetector.ISSUE)

    @Test
    fun `check global scope launch usage`() {
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
                src/GlobalScopeTestCase.kt:6: Warning: Не используйте GlobalScope [GlobalScopeUsage]
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
                
                class GlobalScopeTestCase {
                
                    fun callGlobalScope() {
                        GlobalScope.async {}
                    }
                }
            """.trimIndent()
        )
        val expected =
            """
                src/GlobalScopeTestCase.kt:6: Warning: Не используйте GlobalScope [GlobalScopeUsage]
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
                
                class GlobalScopeTestCase {
                
                    fun callGlobalScope() {
                        GlobalScope.runBlocking {}
                    }
                }
            """.trimIndent()
        )
        val expected =
            """
                src/GlobalScopeTestCase.kt:6: Warning: Не используйте GlobalScope [GlobalScopeUsage]
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
                
                class GlobalScopeTestCase {
                
                    fun callGlobalScope() {
                        val job = GlobalScope.launch {}
                    }
                }
            """.trimIndent()
        )
        val expected =
            """
                src/GlobalScopeTestCase.kt:6: Warning: Не используйте GlobalScope [GlobalScopeUsage]
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
                src/GlobalScopeTestCase.kt:8: Warning: Не используйте GlobalScope [GlobalScopeUsage]
                        scope.launch {}
                        ~~~~~~~~~~~~~~~
                0 errors, 1 warnings
            """.trimIndent()
        check(file, expected)
    }

    private fun check(file: TestFile, expected: String) {
        lintTask.files(file, globalScopeStub)
            .run()
            .expect(expected)
    }

    private val globalScopeStub = LintDetectorTest.kotlin(
        """
            package kotlinx.coroutines
            
            object GlobalScope {
                fun launch()
                fun async()
                fun runBlocking()
            }
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
