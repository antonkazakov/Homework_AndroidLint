import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles

val kotlinxCoroutines: TestFile = TestFiles.kotlin(
    """
            package kotlinx.coroutines
            
            import androidx.lifecycle.CoroutineContextStub
            import kotlin.coroutines.CoroutineContext
            
            interface Job : CoroutineContext
            interface Deffered<Any> : CoroutineContext
            interface CompletableJob : Job
            
            interface CoroutineScope {
                val coroutineContext: CoroutineContext
            }
            
            class JobStub : Job {
                override fun plus(context: CoroutineContext): CoroutineContext = CoroutineContextStub()
            }
            
            class DeferredStub : Deffered<Any> {
                override fun plus(context: CoroutineContext): CoroutineContext = CoroutineContextStub()
            }
            
            fun CoroutineScope.launch(
                context: CoroutineContext,
                block: suspend CoroutineScope.() -> Unit
            ): Job = JobStub()
                
            fun CoroutineScope.async(
                context: CoroutineContext,
                block: suspend CoroutineScope.() -> Unit
            ): Job = DeferredStub()
            
            suspend fun delay(time: Long)
            
            fun SupervisorJob(parent: Job? = null): CompletableJob = JobStub()
            
            fun Job(parent: Job? = null): Job = JobStub()
            
            object Dispatchers {
                val IO = CoroutineContextStub()
            }
            
            object GlobalScope : CoroutineScope
            
            object NonCancellable : Job {
                override fun plus(context: CoroutineContext): CoroutineContext = CoroutineContextStub()
            }
            """.trimIndent()
)

val kotlinCoroutines: TestFile = TestFiles.kotlin(
    """
            package kotlin.coroutines
            
            interface CoroutineContext {
                operator fun plus(context: CoroutineContext): CoroutineContext
            }
            """.trimIndent()
)

val androidxLifecycle: TestFile = TestFiles.kotlin(
    """
            package androidx.lifecycle
            
            import kotlinx.coroutines.CoroutineScope
            import kotlin.coroutines.CoroutineContext
            
            abstract class ViewModel
            
            class CoroutineScopeStub : CoroutineScope {
                override val coroutineContext: CoroutineContext
                    get() = CoroutineContextStub()
            }
            
            class CoroutineContextStub : CoroutineContext {
                override fun plus(context: CoroutineContext): CoroutineContext = CoroutineContextStub()
            }
            
            val ViewModel.viewModelScope: CoroutineScope
                get() = CoroutineScopeStub()
            """.trimIndent()
)