package ru.otus.homework.lintchecks


import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test
import ru.otus.homework.lintchecks.detector.GlobalScopeDetector

@Suppress("UnstableApiUsage")
internal class GlobalScopeDetectorTest {

    private val lintTask = lint().allowMissingSdk().issues(GlobalScopeDetector.ISSUE)

    @Test
    fun `should find globalScope`() {
        lintTask
            .files(
                kotlin(
                    """
                        package test
                        class TestClass {
                            fun onCreate() {
                                GlobalScope.launch{ }
                            }
                        }
                    """.trimIndent()
                )
            )
            .run()
            .expectWarningCount(1)
    }

    @Test
    fun `should find GlobalScope in ViewModel`() {
        lintTask
            .files(
                kotlin(
                    """
                        package test
                        class TestViewModel : ViewModel() {
                            fun onCreate() {
                                GlobalScope.launch{ }
                            }
                        }
                    """.trimIndent()
                )
            )
            .run()
            .expectWarningCount(1)
    }
}