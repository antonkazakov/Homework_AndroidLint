package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

internal class GlobalScopeDetectorTest {

    @Test
    fun `Testing GlobalScope usage output`() {
        lint()
            .allowMissingSdk()
            .files(
                kotlin(
                    """
                        package tests
                        val scope = GlobalScope
                        class ViewModelTest {
                            init {
                                GlobalScope.launch{ }
                            }
                        }
                    """.trimIndent()
                )
            )
            .issues(GlobalScopeDetector.ISSUE)
            .run()
            .expect(
                """
                    src/tests/ViewModelTest.kt:2: Error: Использование GlobalScope не допускается [GlobalScopeUsage]
                    val scope = GlobalScope
                                ~~~~~~~~~~~
                    src/tests/ViewModelTest.kt:5: Error: Использование GlobalScope не допускается [GlobalScopeUsage]
                            GlobalScope.launch{ }
                            ~~~~~~~~~~~
                    2 errors, 0 warnings
                """.trimIndent()
            )
    }
}