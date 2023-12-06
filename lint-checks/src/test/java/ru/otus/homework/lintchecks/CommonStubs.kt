package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest

object CommonStubs {

    val globalScopeStub = LintDetectorTest.kotlin(
        """
            package kotlinx.coroutines
            
            interface CoroutineScope
            object GlobalScope : CoroutineScope
            
            fun CoroutineScope.launch(block: suspend () -> Unit) {}
            fun CoroutineScope.async(block: suspend () -> Unit) {}
            fun CoroutineScope.runBlocking(block: suspend () -> Unit) {}
            fun delay(timeMillis: Long) {}
        """.trimIndent()
    )

    val channelsStub = LintDetectorTest.kotlin(
        """
            package kotlinx.coroutines.channels
            
            import kotlinx.coroutines.*
            
            fun <E> CoroutineScope.actor(block: suspend () -> Unit)
        """.trimIndent()
    )

    val viewModelStub = LintDetectorTest.kotlin(
        """
            package androidx.lifecycle
            
            import kotlinx.coroutines.CoroutineScope
            
            abstract class ViewModel    
            val ViewModel.viewModelScope: CoroutineScope
        """.trimIndent()
    )
}