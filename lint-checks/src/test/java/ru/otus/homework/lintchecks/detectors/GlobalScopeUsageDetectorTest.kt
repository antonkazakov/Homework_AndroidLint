package ru.otus.homework.lintchecks.detectors

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test

@Suppress("UnstableApiUsage")
class GlobalScopeUsageDetectorTest {

    @Test
    fun `Testing GlobalScope usage output`() {
        lint()
            .allowMissingSdk()
            .files(
                kotlin(
                    """
                        @file:OptIn(DelicateCoroutinesApi::class)
package ru.otus.homework.linthomework.globalscopeusage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
            .allowCompilationErrors()
            .testModes(TestMode.DEFAULT)
            .issues(GlobalScopeUsageDetector.ISSUE)
            .run()
            .expect(
                """
src/ru/otus/homework/linthomework/globalscopeusage/GlobalScopeTestCase.kt:17: Warning: Использовать GlobalScopeUsage не рекомендуется [GlobalScopeUsage]
        GlobalScope.launch {
        ~~~~~~~~~~~
src/ru/otus/homework/linthomework/globalscopeusage/GlobalScopeTestCase.kt:21: Warning: Использовать GlobalScopeUsage не рекомендуется [GlobalScopeUsage]
        GlobalScope.actor<String> {
        ~~~~~~~~~~~
src/ru/otus/homework/linthomework/globalscopeusage/GlobalScopeTestCase.kt:29: Warning: Использовать GlobalScopeUsage не рекомендуется [GlobalScopeUsage]
            val deferred = GlobalScope.async {
                           ~~~~~~~~~~~
0 errors, 3 warnings
                """.trimIndent()
            )
    }
}