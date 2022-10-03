package ru.otus.homework.detectors

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test
import ru.otus.homework.detectors.stubs.coroutinesKotlinX
import ru.otus.homework.detectors.stubs.viewModelFile
import ru.otus.homework.detectors.stubs.viewModelScope
import ru.otus.homework.lintchecks.detectors.GlobalScopeUsagesDetector

class GlobalScopeUsagesDetectorTest {

    private val testLint = TestLintTask
        .lint()
        .allowMissingSdk()
        .issues(GlobalScopeUsagesDetector.ISSUE)

    @Test
    fun testBasic() {
        testLint.files(
            kotlin(
                """                    
                    package ru.otus.homework.linthomework.globalscopeusage
                    
                    import android.app.Activity
                    import androidx.lifecycle.ViewModel
                    import androidx.lifecycle.viewModelScope
                    import kotlinx.coroutines.CoroutineScope
                    import kotlinx.coroutines.GlobalScope
                    
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
            ).indented(), viewModelFile, viewModelScope, coroutinesKotlinX
        )
            .run()
            .expect(expectedValue)
    }

    private val expectedValue =
        """src/ru/otus/homework/linthomework/globalscopeusage/GlobalScopeTestCase.kt:12: Error: BriefDescription - Don't use Global Scope [GlobalScopeUsage]
        GlobalScope.launch {
        ~~~~~~~~~~~
src/ru/otus/homework/linthomework/globalscopeusage/GlobalScopeTestCase.kt:16: Error: BriefDescription - Don't use Global Scope [GlobalScopeUsage]
        GlobalScope.actor<String> {
        ~~~~~~~~~~~
src/ru/otus/homework/linthomework/globalscopeusage/GlobalScopeTestCase.kt:24: Error: BriefDescription - Don't use Global Scope [GlobalScopeUsage]
            val deferred = GlobalScope.async {
                           ~~~~~~~~~~~
3 errors, 0 warnings
        """.trimIndent()
}