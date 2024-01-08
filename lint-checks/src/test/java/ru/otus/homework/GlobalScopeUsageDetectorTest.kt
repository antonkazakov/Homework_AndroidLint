package ru.otus.homework

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFiles
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test
import ru.otus.homework.lintchecks.GlobalScopeUsageDetector

class GlobalScopeUsageDetectorTest {

    private val testLintTask = TestLintTask.lint()
        .allowMissingSdk()
        .issues(GlobalScopeUsageDetector.ISSUE)

    private val viewModelStub = TestFiles.java(
        """
            package androidx.lifecycle;
            
            public abstract class ViewModel {}    
        """.trimIndent()
    )

    private val viewModelExtensionsStub = TestFiles.kotlin(
        """
            package androidx.lifecycle

            import kotlinx.coroutines.CoroutineScope
            public val ViewModel.viewModelScope: CoroutineScope    
        """.trimIndent()
    )

    private val coroutineStub = TestFiles.kotlin(
        """
            package kotlinx.coroutines
            
            object GlobalScope: CoroutineScope
            public interface CoroutineScope
            fun CoroutineScope.launch(block: suspend () -> Unit) {}
            fun CoroutineScope.async(block: suspend () -> Unit) {}
            suspend fun delay(timeMillis: Long) {}
        """.trimIndent()
    )

    private val channelsStub = TestFiles.kotlin(
        """
            package kotlinx.coroutines.channels
            import kotlinx.coroutines.*
            
            public fun <E> CoroutineScope.actor(
                block: suspend () -> Unit
            ) {}
        """.trimIndent()
    )

    @Test
    fun `check two usage global scope`() {
        testLintTask
            .files(
                LintDetectorTest.kotlin(
                    """
                    package ru.otus.homework.linthomework.globalscopeusage
                    
                    import androidx.lifecycle.ViewModel
                    import kotlinx.coroutines.GlobalScope
                    import kotlinx.coroutines.channels.actor
                    import kotlinx.coroutines.delay
                    import kotlinx.coroutines.launch
                    
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
                ), viewModelStub, coroutineStub, channelsStub
            )
            .run()
            .expectWarningCount(2)
            .expect(
                """
                    src/ru/otus/homework/linthomework/globalscopeusage/GlobalScopeTestCase.kt:12: Warning: Использование GlobalScope может привести к утечкам памяти [GlobalScopeUsage]
                            GlobalScope.launch {
                            ^
                    src/ru/otus/homework/linthomework/globalscopeusage/GlobalScopeTestCase.kt:16: Warning: Использование GlobalScope может привести к утечкам памяти [GlobalScopeUsage]
                            GlobalScope.actor<String> {
                            ^
                    0 errors, 2 warnings
                """.trimIndent()
            )
    }

    @Test
    fun `check usage global scope into view model scope`() {
        testLintTask
            .files(
                LintDetectorTest.kotlin(
                    """
                        package ru.otus.homework.linthomework.globalscopeusage

                        import androidx.lifecycle.ViewModel
                        import androidx.lifecycle.viewModelScope
                        import kotlinx.coroutines.CoroutineScope
                        import kotlinx.coroutines.GlobalScope
                        import kotlinx.coroutines.async
                        import kotlinx.coroutines.delay
                        import kotlinx.coroutines.launch
                        
                        class GlobalScopeTestCase(private val scope: CoroutineScope) : ViewModel() {
                        
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
                ), viewModelStub, coroutineStub, viewModelExtensionsStub
            )
            .run()
            .expectWarningCount(1)
            .expect(
                """
                    src/ru/otus/homework/linthomework/globalscopeusage/GlobalScopeTestCase.kt:15: Warning: Использование GlobalScope может привести к утечкам памяти [GlobalScopeUsage]
                                val deferred = GlobalScope.async {
                                               ^
                    0 errors, 1 warnings
                """.trimIndent()
            )
    }

    @Test
    fun `check positive case without global scope usages`() {
        testLintTask
            .files(
                LintDetectorTest.kotlin(
                    """
                        package ru.otus.homework.linthomework.globalscopeusage

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
                ), viewModelStub, coroutineStub, viewModelExtensionsStub
            )
            .run()
            .expectClean()
    }
}