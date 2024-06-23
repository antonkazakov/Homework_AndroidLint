@file:Suppress("UnstableApiUsage")

package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GlobalScopeDetectorTest {

    @Test
    fun testFragmentGlobalScope() {
        lint()
            .files(
                kotlin(
                    """
                package ru.otus.homework.linthomework.globalscopeusage

                import androidx.fragment.app.Fragment
                import kotlinx.coroutines.GlobalScope
                import kotlinx.coroutines.launch

                class GlobalScopeTestCaseFragment : Fragment() {
                    init {
                        GlobalScope.launch {
                        }
                    }
                }
                """
                ).indented(),
                kotlin(
                    """
                    package androidx.fragment.app
                    interface Fragment
                """
                ).indented(),
                kotlin(
                    """
                    package kotlinx.coroutines
                    interface CoroutineScope 
                    object GlobalScope: CoroutineScope
                    fun CoroutineScope.launch(block: suspend () -> Unit) {}
                """
                ).indented()
            )
            .allowMissingSdk()
            .testModes(TestMode.DEFAULT)
            .detector(GlobalScopeDetector())
            .issues(GlobalScopeDetector.ISSUE)
            .run()
            .expect(
                """
            src/ru/otus/homework/linthomework/globalscopeusage/GlobalScopeTestCaseFragment.kt:9: Warning: Использование GlobalScope может приводить к утечкам памяти. [GlobalScopeUsage]
                    GlobalScope.launch {
                    ~~~~~~~~~~~
            0 errors, 1 warnings"""
            )
    }
}
