package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test

internal class GlobalScopeDetectorTest {

    private val lintTask = TestLintTask
        .lint()
        .allowMissingSdk()
        .issues(GlobalScopeDetector.ISSUE)

    @Test
    fun `Test1 GlobalScope`() {
        lintTask
            .files(
                kotlin(
                    """
                    fun testFun() {
                        MyGlobalScope.launch {}
                    }
                    class TestClass() : ViewModel() {
                            fun case1() {
                                GlobalScope_.launch {}
                            }
                        }
                """.trimIndent()
                )
            )
            .run()
            .expect("""No warnings.""".trimIndent())
    }

    @Test
    fun `Test2 GlobalScope`() {
        lintTask
            .files(
                kotlin(
                    """
                    fun testFun() {
                        GlobalScope.launch {}
                    }fun case1() {
                        GlobalScope.launch {}
                        GlobalScope.actor<String> {}
                    }
                """.trimIndent()
                )
            )
            .run()
            .expect("""
                src/test.kt:2: Warning: danger GlobalScope using [GlobalScopeUsage]
                    GlobalScope.launch {}
                    ~~~~~~~~~~~
                src/test.kt:4: Warning: danger GlobalScope using [GlobalScopeUsage]
                    GlobalScope.launch {}
                    ~~~~~~~~~~~
                src/test.kt:5: Warning: danger GlobalScope using [GlobalScopeUsage]
                    GlobalScope.actor<String> {}
                    ~~~~~~~~~~~
                0 errors, 3 warnings
            """.trimIndent())
    }
}
