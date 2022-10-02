package ru.otus.homework.detectors.stubs

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.java
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestFile

val coroutines: TestFile = kotlin(
    """
        package kotlin.coroutines

        public interface CoroutineContext {
            public operator fun <E : Element> get(key: Key<E>): E?

            public fun <R> fold(initial: R, operation: (R, Element) -> R): R

            public operator fun plus(context: CoroutineContext): CoroutineContext       

            public fun minusKey(key: Key<*>): CoroutineContext

            public interface Key<E : Element>

            public interface Element : CoroutineContext {
                
                public val key: Key<*>

                public override operator fun <E : Element> get(key: Key<E>): E? 

                public override fun <R> fold(initial: R, operation: (R, Element) -> R): R

                public override fun minusKey(key: Key<*>): CoroutineContext
            }
        }
    """.trimIndent()
)

val coroutinesKotlinX: TestFile = kotlin(
    """
        package kotlinx.coroutines
        
        import kotlin.coroutines.*
        
        public interface CoroutineScope {
            public val coroutineContext: CoroutineContext
        }

        public object GlobalScope : CoroutineScope {
            override val coroutineContext: CoroutineContext
        }

        public fun CoroutineScope.launch(
            context: CoroutineContext = EmptyCoroutineContext,
            start: CoroutineStart = CoroutineStart.DEFAULT,
            block: suspend CoroutineScope.() -> Unit
        ): Job

        public object EmptyCoroutineContext : CoroutineContext, Serializable {
            private const val serialVersionUID: Long = 0
            private fun readResolve(): Any = EmptyCoroutineContext
        
            public override fun <E : Element> get(key: Key<E>): E? = null
            public override fun <R> fold(initial: R, operation: (R, Element) -> R): R = initial
            public override fun plus(context: CoroutineContext): CoroutineContext = context
            public override fun minusKey(key: Key<*>): CoroutineContext = this
            public override fun hashCode(): Int = 0
            public override fun toString(): String = "EmptyCoroutineContext"
        }

        public actual object Dispatchers {

            @JvmStatic
            public actual val Default: CoroutineDispatcher
            
            @JvmStatic
            public actual val Main: MainCoroutineDispatcher get() {}
        
          
            @JvmStatic
            public actual val Unconfined: CoroutineDispatcher get() {}
        
            @JvmStatic
            public val IO: CoroutineDispatcher get() {}
        
            public fun shutdown() {
            }
        }
        
        public interface Job : CoroutineContext.Element {
            public companion object Key : CoroutineContext.Key<Job>

            public val isActive: Boolean

            public val isCompleted: Boolean

            public val isCancelled: Boolean

            public fun getCancellationException(): CancellationException

            public fun start(): Boolean

            public fun cancel(cause: CancellationException? = null)

            @Deprecated(level = DeprecationLevel.HIDDEN, message = "Since 1.2.0, binary compatibility with versions <= 1.1.x")
            public fun cancel(): Unit = cancel(null)

            @Deprecated(level = DeprecationLevel.HIDDEN, message = "Since 1.2.0, binary compatibility with versions <= 1.1.x")
            public fun cancel(cause: Throwable? = null): Boolean

            public val children: Sequence<Job>

            public fun attachChild(child: ChildJob): ChildHandle

            public suspend fun join()

            public val onJoin: SelectClause0

            public fun invokeOnCompletion(handler: CompletionHandler): DisposableHandle
   
            @InternalCoroutinesApi
            public fun invokeOnCompletion(
                onCancelling: Boolean = false,
                invokeImmediately: Boolean = true,
                handler: CompletionHandler): DisposableHandle

            @Suppress("DeprecatedCallableAddReplaceWith")
            @Deprecated(message = "Operator '+' on two Job objects is meaningless. " +
                "Job is a coroutine context element and `+` is a set-sum operator for coroutine contexts. " +
                "The job to the right of `+` just replaces the job the left of `+`.",
                level = DeprecationLevel.ERROR)
            public operator fun plus(other: Job): Job
        }
        
        @Suppress("DeprecatedCallableAddReplaceWith")
        public object NonCancellable : AbstractCoroutineContextElement(Job), Job {

            private const val message = "NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited"

            @Deprecated(level = DeprecationLevel.WARNING, message = message)
            override val isActive: Boolean
                get() = true

            @Deprecated(level = DeprecationLevel.WARNING, message = message)
            override val isCompleted: Boolean get() = false

            @Deprecated(level = DeprecationLevel.WARNING, message = message)
            override val isCancelled: Boolean get() = false

            @Deprecated(level = DeprecationLevel.WARNING, message = message)
            override fun start(): Boolean = false

            @Deprecated(level = DeprecationLevel.WARNING, message = message)
            override suspend fun join() {
                throw UnsupportedOperationException("This job is always active")
            }

            @Deprecated(level = DeprecationLevel.WARNING, message = message)
            override val onJoin: SelectClause0
                get() = throw UnsupportedOperationException("This job is always active")

            @Deprecated(level = DeprecationLevel.WARNING, message = message)
            override fun getCancellationException(): CancellationException = throw IllegalStateException("This job is always active")

            @Deprecated(level = DeprecationLevel.WARNING, message = message)
            override fun invokeOnCompletion(handler: CompletionHandler): DisposableHandle
            
            @Deprecated(level = DeprecationLevel.WARNING, message = message)
            override fun invokeOnCompletion(onCancelling: Boolean, invokeImmediately: Boolean, handler: CompletionHandler): DisposableHandle

            @Deprecated(level = DeprecationLevel.WARNING, message = message)
            override fun cancel(cause: CancellationException?) {}

            @Deprecated(level = DeprecationLevel.HIDDEN, message = "Since 1.2.0, binary compatibility with versions <= 1.1.x")
            override fun cancel(cause: Throwable?): Boolean = false // never handles exceptions

            @Deprecated(level = DeprecationLevel.WARNING, message = message)
            override val children: Sequence<Job>
                get() = emptySequence()

            @Deprecated(level = DeprecationLevel.WARNING, message = message)
            override fun attachChild(child: ChildJob): ChildHandle

            /** @suppress */
            override fun toString(): String
        }
        
        @Suppress("FunctionName")
        public fun SupervisorJob(parent: Job? = null) : CompletableJob
        
        public interface CompletableJob : Job {
           
            public fun complete(): Boolean
           
            public fun completeExceptionally(exception: Throwable): Boolean
        }
        
        public enum class CoroutineStart {
            DEFAULT,

            LAZY,

            ATOMIC,

            UNDISPATCHED;

            public operator fun <T> invoke(block: suspend () -> T, completion: Continuation<T>): Unit
            public operator fun <R, T> invoke(block: suspend R.() -> T, receiver: R, completion: Continuation<T>): Unit 
            public val isLazy: Boolean
        }

    """.trimIndent()
).indented()

val viewModelScope: TestFile = kotlin(
    """
        package androidx.lifecycle
        
        public val ViewModel.viewModelScope: CoroutineScope
            get() {
            }

    """.trimIndent()
).indented()

val viewModelFile: TestFile = java(
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

