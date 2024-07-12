@file:Suppress("UnstableApiUsage")

package ru.otus.homework.lintchecks.detectors

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin

object Stubs {
    val viewModelStub = LintDetectorTest.java(
        """
    package androidx.lifecycle;
    
    public abstract class ViewModel {}    
    """.trimIndent()
    )

    val viewModelExtensionsStub = kotlin(
        """
    package androidx.lifecycle
    import kotlinx.coroutines.CoroutineScope
    public val ViewModel.viewModelScope: CoroutineScope    
    """.trimIndent()
    )

    val coroutineStub = kotlin(
        """
    package kotlinx.coroutines
    
    public interface CoroutineScope
    fun CoroutineScope.launch(block: suspend () -> Unit) {}
    fun CoroutineScope.async(block: suspend () -> Unit) {}
    suspend fun delay(timeMillis: Long) {}
    public actual object Dispatchers {
        public val IO: Any = Any()
    }
    public interface Job {}
    public interface CompletableJob : Job{}
    private class SupervisorJobImpl(parent: Job?) : Job {  }
    public fun SupervisorJob(parent: Job? = null) : CompletableJob = SupervisorJobImpl(parent)
    public object NonCancellable : Job {}
    internal open class JobImpl(parent: Job?) : CompletableJob { }
    public fun Job(parent: Job? = null): CompletableJob = JobImpl(parent)
    """.trimIndent()
    )

}