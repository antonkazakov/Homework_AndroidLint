package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test

class GlobalScopeDetectorTest {
    private val lintTask = TestLintTask.lint()
        .allowMissingSdk()
        .issues(GlobalScopeDetector.ISSUE)

    private val globalScopeStub = kotlin(
        """
            package kotlinx.coroutines
            
            public interface CoroutineScope {
                public val coroutineContext: CoroutineContext
            }


            public object GlobalScope : CoroutineScope {
                override val coroutineContext: CoroutineContext
                    get() = EmptyCoroutineContext
                
                public fun launch(
                    context: CoroutineContext = EmptyCoroutineContext,
                    start: CoroutineStart = CoroutineStart.DEFAULT,
                    block: suspend CoroutineScope.() -> Unit
                ): Job
            }
        """.trimIndent()
    )

    @Test
    fun `should detect GlobalScope usage in kotlin`() {
        lintTask
            .files(
                LintDetectorTest.kotlin(
                    """
                package test.pkg
                
                import kotlinx.coroutines.GlobalScope
    
                class Test2 {
                    
                    fun foo() {
                        GlobalScope.launch {}
                    }
                }               
            """.trimIndent()
                ), globalScopeStub
            )
            .run()
            .expect(
                """src/test/pkg/Test2.kt:8: Warning: Замените GlobalScope на другой CoroutineScope [GlobalScopeUsage]
        GlobalScope.launch {}
        ~~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warnings""".trimIndent()
            )
    }
}