package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestLintTask
import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CoroutineJobDetectorTest {

    @Test
    fun checkJob() {
        TestLintTask.lint()
            .files(
                LintDetectorTest.kotlin(
                    """
                package ru.otus.homework.linthomework.jobinbuilderusage

                import androidx.lifecycle.ViewModel
                import androidx.lifecycle.viewModelScope
                import kotlinx.coroutines.Job
                import kotlinx.coroutines.SupervisorJob
                import kotlinx.coroutines.launch
                import kotlinx.coroutines.Dispatchers

                class VM(
                    private val job: Job
                ) : ViewModel() {
                
                    fun case1() {
                        viewModelScope.launch(SupervisorJob()) {
                        }
                    }
                }
                """
                ).indented(),
                LintDetectorTest.kotlin(
                    """
                    package kotlinx.coroutines
                    
                    interface CoroutineContext {
                        operator fun <E : Element> get(key: Key<E>): E?
                        fun <R> fold(initial: R, operation: (R, Element) -> R): R
                        
                        operator fun plus(context: CoroutineContext): CoroutineContext =
                            if (context === EmptyCoroutineContext) this else // fast path -- avoid lambda creation
                                context.fold(this) { acc, element ->
                                    val removed = acc.minusKey(element.key)
                                    if (removed === EmptyCoroutineContext) element else {
                                        // make sure interceptor is always last in the context (and thus is fast to get when present)
                                        val interceptor = removed[ContinuationInterceptor]
                                        if (interceptor == null) CombinedContext(removed, element) else {
                                            val left = removed.minusKey(ContinuationInterceptor)
                                            if (left === EmptyCoroutineContext) CombinedContext(element, interceptor) else
                                                CombinedContext(CombinedContext(left, element), interceptor)
                                        } 
                                    } 
                                }
                                
                        fun minusKey(key: Key<*>): CoroutineContext
                        interface Key<E : Element>
                        interface Element : CoroutineContext {
                            val key: Key<*>
                            override operator fun <E : Element> get(key: Key<E>): E? =
                                if (this.key == key) this as E else null
                            override fun <R> fold(initial: R, operation: (R, Element) -> R): R =
                                operation(initial, this)

                            override fun minusKey(key: Key<*>): CoroutineContext =
                                if (this.key == key) EmptyCoroutineContext else this
                        }
                    }
    
                    interface Job
                    class CompletableJob : Job
                    class CoroutineScope
                    fun SupervisorJob() : CompletableJob = Job()
                    fun Job(): CompletableJob
                    object NonCancellable : Job
                    fun CoroutineScope.launch(context: CoroutineContext, block: suspend () -> Unit) {}
                    open class CoroutineDispatcher
                    object Dispatchers {
                        val IO: CoroutineDispatcher = CoroutineDispatcher()
                    }
                """
                ).indented(),
                LintDetectorTest.kotlin(
                    """
                    package androidx.lifecycle
                    import kotlinx.coroutines.CoroutineScope
                    interface ViewModel
                    val ViewModel.viewModelScope = CoroutineScope()
                """
                ).indented()
            )
            .allowMissingSdk()
            .testModes(TestMode.DEFAULT)
            .detector(CoroutineJobDetector())
            .issues(CoroutineJobDetector.ISSUE)
            .run()
            .expect("""
                src/ru/otus/homework/linthomework/jobinbuilderusage/VM.kt:15: Warning: Использование Job в коуртин билдерах [JobInBuilderUsage]
        viewModelScope.launch(SupervisorJob()) {
        ^
0 errors, 1 warnings
            """.trimIndent())
    }
}