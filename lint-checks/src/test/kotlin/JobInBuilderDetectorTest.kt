import com.android.tools.lint.checks.infrastructure.TestFiles.java
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test
import ru.otus.homework.lintchecks.GlobalScopeDetector
import ru.otus.homework.lintchecks.JobInBuilderDetector

class JobInBuilderDetectorTest {

    private val coroutineScopeStub = kotlin(
        """
            @file:OptIn(ExperimentalContracts::class)
            package kotlinx.coroutines
import kotlinx.atomicfu.*
import kotlinx.coroutines.internal.*
import kotlinx.coroutines.intrinsics.*
import kotlinx.coroutines.selects.*
import kotlin.contracts.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
import kotlin.jvm.*


            public fun CoroutineScope.launch(
                context: CoroutineContext = EmptyCoroutineContext,
                start: CoroutineStart = CoroutineStart.DEFAULT,
                block: suspend CoroutineScope.() -> Unit
            ): Job {
                val newContext = newCoroutineContext(context)
                val coroutine = if (start.isLazy)
                    LazyStandaloneCoroutine(newContext, block) else
                    StandaloneCoroutine(newContext, active = true)
                coroutine.start(start, coroutine, block)
                return coroutine
            }

            public fun SupervisorJob(parent: Job? = null) : CompletableJob = SupervisorJobImpl(parent)

            public actual object Dispatchers {}

            public interface CoroutineScope {
    
                public val coroutineContext: CoroutineContext
            }
        """.trimIndent()
    )

    private val viewModelStub = java(
        """
            package androidx.lifecycle;
            public abstract class ViewModel {
    
            }
        """.trimIndent()
    )

    private val viewModelScopeStub = kotlin(
        """
            package androidx.lifecycle

            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.Dispatchers
            import kotlinx.coroutines.SupervisorJob
            import java.io.Closeable
            import kotlin.coroutines.CoroutineContext

            private const val JOB_KEY = "androidx.lifecycle.ViewModelCoroutineScope.JOB_KEY"

            public val ViewModel.viewModelScope: CoroutineScope
                get() {
                    val scope: CoroutineScope? = this.getTag(JOB_KEY)
                    if (scope != null) {
                       return scope
                    }
                    return setTagIfAbsent(
                        JOB_KEY,
                        CloseableCoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
                    )
                }

            internal class CloseableCoroutineScope(context: CoroutineContext) : Closeable, CoroutineScope {
                override val coroutineContext: CoroutineContext = context

                override fun close() {
                    
                }
            }   
        """.trimIndent()
    )

    private val lintTask = TestLintTask.lint()
        .allowMissingSdk()
        .issues(JobInBuilderDetector.ISSUE)

    @Test
    fun jobInBuilderDetectorViewModelTest1(){
        lintTask
            .files(
                kotlin(
                    """
                        package ru.otus.homework.linthomework.jobinbuilderusage
                        import kotlinx.coroutines.launch
                        import androidx.lifecycle.ViewModel
                        import androidx.lifecycle.viewModelScope
                        import kotlinx.coroutines.SupervisorJob
                        import kotlinx.coroutines.Dispatchers

                        class JobTest: ViewModel(){
                            
                            fun supervisorJobUsage(){
                                viewModelScope.launch(SupervisorJob() + Dispatchers.IO) {
                                    println("Hello World")
                                }
                            }
                        }
                    """.trimIndent()
                ), coroutineScopeStub, viewModelScopeStub, viewModelStub
            )
            .run()
            .expect("""
                src/ru/otus/homework/linthomework/jobinbuilderusage/JobTest.kt:11: Error: Использование внутри корутин-билдеров не имеет никакого эффекта, это может сломать ожидаемые обработку ошибок и механизм отмены корутин [JobInBuilderUsage]
        viewModelScope.launch(SupervisorJob() + Dispatchers.IO) {
                              ~~~~~~~~~~~~~~~
1 errors, 0 warnings
            """.trimIndent())
    }

    @Test
    fun jobInBuilderDetectorViewModelTest2(){
        lintTask
            .files(
                kotlin(
                    """
                        package ru.otus.homework.linthomework.jobinbuilderusage
                        import kotlinx.coroutines.launch
                        import androidx.lifecycle.ViewModel
                        import androidx.lifecycle.viewModelScope
                        import kotlinx.coroutines.SupervisorJob
                        import kotlinx.coroutines.Dispatchers

                        class JobTest: ViewModel(){
                            
                            fun jobUsage(){
                                viewModelScope.launch(Job()) {
                                    println("Hello World")
                                }
                            }
                        }
                    """.trimIndent()
                ), coroutineScopeStub, viewModelScopeStub, viewModelStub
            )
            .run()
            .expect("""
                src/ru/otus/homework/linthomework/jobinbuilderusage/JobTest.kt:11: Error: Использование внутри корутин-билдеров не имеет никакого эффекта, это может сломать ожидаемые обработку ошибок и механизм отмены корутин [JobInBuilderUsage]
        viewModelScope.launch(Job()) {
                              ~~~~~
1 errors, 0 warnings
            """.trimIndent())
    }
}