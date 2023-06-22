package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFiles
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test

class JobInBuilderDetectorTest {
    private val lintTask = TestLintTask.lint()
        .allowMissingSdk()
        .issues(JobInBuilderDetector.ISSUE)

    private val coroutineContextStub = TestFiles.kotlin(
        """
            package kotlin.coroutines

            public interface CoroutineContext {
                /**
                 * Returns the element with the given [key] from this context or `null`.
                 */
                public operator fun <E : Element> get(key: Key<E>): E?

                /**
                 * Accumulates entries of this context starting with [initial] value and applying [operation]
                 * from left to right to current accumulator value and each element of this context.
                 */
                public fun <R> fold(initial: R, operation: (R, Element) -> R): R

                /**
                 * Returns a context containing elements from this context and elements from  other [context].
                 * The elements from this context with the same key as in the other one are dropped.
                 */
                public operator fun plus(context: CoroutineContext): CoroutineContext =
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

                /**
                 * Returns a context containing elements from this context, but without an element with
                 * the specified [key].
                 */
                public fun minusKey(key: Key<*>): CoroutineContext

                /**
                 * Key for the elements of [CoroutineContext]. [E] is a type of element with this key.
                 */
                public interface Key<E : Element>

                /**
                 * An element of the [CoroutineContext]. An element of the coroutine context is a singleton context by itself.
                 */
                public interface Element : CoroutineContext {
                    /**
                     * A key of this coroutine context element.
                     */
                    public val key: Key<*>

                    public override operator fun <E : Element> get(key: Key<E>): E? =
                        @Suppress("UNCHECKED_CAST")
                        if (this.key == key) this as E else null

                    public override fun <R> fold(initial: R, operation: (R, Element) -> R): R =
                        operation(initial, this)

                    public override fun minusKey(key: Key<*>): CoroutineContext =
                        if (this.key == key) EmptyCoroutineContext else this
                }
            }
        """.trimIndent()
    )

    private val coroutineScopeStub = TestFiles.kotlin(
        """
            package kotlinx.coroutines
            
            import kotlin.coroutines.CoroutineContext
            
            class CoroutineScope {
                public val coroutineContext: CoroutineContext
                
                public fun launch(
                    context: CoroutineContext = EmptyCoroutineContext,
                    start: CoroutineStart = CoroutineStart.DEFAULT,
                    block: suspend CoroutineScope.() -> Unit
                ): Job
                
                fun <T> async(
                    context: CoroutineContext = EmptyCoroutineContext,
                    start: CoroutineStart = CoroutineStart.DEFAULT,
                    block: suspend CoroutineScope.() -> T
                ): Deferred<T>
            }
    
            class Job {}
    
            object NonCancellable: Job()
        """.trimIndent()
    )

    @Test
    fun `should detect Job usage in coroutine builders`() {
        lintTask
            .files(
                LintDetectorTest.kotlin(
                    """
                package test.pkg
                
                import kotlinx.coroutines.CoroutineScope
                import kotlinx.coroutines.Job
    
                class Test2 {
                    val coroutineScope = CoroutineScope()
                    
                    fun foo() {
                        coroutineScope.launch(Job()) {}
                    }
                } 
                    """.trimIndent()
                ), coroutineScopeStub, coroutineContextStub
            )
            .run()
            .expect(
                """src/test/pkg/Test2.kt:10: Warning: Удалите экземпляр Job из coroutine builder [JobInBuilderUsage]
        coroutineScope.launch(Job()) {}
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warnings""".trimIndent()
            )
    }
}