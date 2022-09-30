package ru.otus.homework.detectors

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.java
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test
import ru.otus.homework.lintchecks.detectors.GlobalScopeUsagesDetector

class GlobalScopeUsagesDetectorTest {

    private val coroutineScope = kotlin(
        """
        package kotlinx.coroutines
        
        public interface CoroutineScope {
            public val coroutineContext: CoroutineContext
        }

        public object GlobalScope : CoroutineScope {
            /**
             * Returns [EmptyCoroutineContext].
             */
            override val coroutineContext: CoroutineContext
        }
    """.trimIndent()
    ).indented()

    private val viewModelScope = kotlin(
        """
        package androidx.lifecycle

        public val ViewModel.viewModelScope: CoroutineScope
    """.trimIndent()
    ).indented()

    private val viewModelFile = java(
        """
        package androidx.lifecycle;

        public abstract class ViewModel {
            
            @SuppressWarnings("WeakerAccess")
            protected void onCleared() {
            }

            @MainThread
            final void clear() {
            } 
                   
            @SuppressWarnings("unchecked")
            <T> T setTagIfAbsent(String key, T newValue) {
            }

            /**
             * Returns the tag associated with this viewmodel and the specified key.
             */
            @SuppressWarnings({"TypeParameterUnusedInFormals", "unchecked"})
            <T> T getTag(String key) {
            }
        }
    """.trimIndent()
    ).indented()

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
            ).indented(), viewModelFile, viewModelScope, coroutineScope
        )
            .run()
            .expect(expectedValue)
    }

    private val expectedValue =
        """src/ru/otus/homework/linthomework/globalscopeusage/GlobalScopeTestCase.kt:12: Error: BriefDescription - GlobalScopeUsage [GlobalScopeUsage]
        GlobalScope.launch {
        ~~~~~~~~~~~
src/ru/otus/homework/linthomework/globalscopeusage/GlobalScopeTestCase.kt:16: Error: BriefDescription - GlobalScopeUsage [GlobalScopeUsage]
        GlobalScope.actor<String> {
        ~~~~~~~~~~~
src/ru/otus/homework/linthomework/globalscopeusage/GlobalScopeTestCase.kt:24: Error: BriefDescription - GlobalScopeUsage [GlobalScopeUsage]
            val deferred = GlobalScope.async {
                           ~~~~~~~~~~~
3 errors, 0 warnings
        """.trimIndent()
}