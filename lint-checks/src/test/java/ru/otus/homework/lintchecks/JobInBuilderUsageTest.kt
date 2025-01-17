package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test

@Suppress("UnstableApiUsage")
internal class JobInBuilderUsageTest {

    private val lintTask = lint().allowMissingSdk().issues(JobInBuilderUsage.ISSUE)

    @Test
    fun testJobInBuilderUsage() {
        lintTask.files(
            kotlin(
                """
                package ru.otus.homework.linthomework.jobinbuilderusage
                import androidx.lifecycle.ViewModel
                import androidx.lifecycle.viewModelScope
                import kotlinx.coroutines.Dispatchers
                import kotlinx.coroutines.Job
                import kotlinx.coroutines.NonCancellable
                import kotlinx.coroutines.SupervisorJob
                import kotlinx.coroutines.launch
                class JobInBuilderTestCase(
                    private val job: Job
                ) : ViewModel() {
                    fun case1() {
                        viewModelScope.launch(SupervisorJob() + Dispatchers.IO) {
                            println("Hello World")
                        }
                    }
                    fun case2() {
                        viewModelScope.launch(Job()) {
                            println("Hello World")
                        }
                    }
                    fun case3() {
                        viewModelScope.launch(job) {
                            println("Hello World")
                        }
                    }
                    fun case4() {
                        viewModelScope.launch(NonCancellable) {
                            println("Hello World")
                        }
                    }
                    fun case5() {
                        viewModelScope.launch(SupervisorJob()) {
                            println("Hello World")
                        }
                    }
                }
            """.trimIndent()
            ),
            kotlin(
                """
                package androidx.lifecycle
                import kotlinx.coroutines.CoroutineScope
                import kotlin.coroutines.CoroutineContext
                public abstract class ViewModel
                class CoroutineScopeStub: CoroutineScope {
                    override val coroutineContext: CoroutineContext
                        get() = CoroutineContextStub()
                }
                class CoroutineContextStub: CoroutineContext {
                    override fun plus(context: CoroutineContext): CoroutineContext = CoroutineContextStub()
                }
                public val ViewModel.viewModelScope: CoroutineScope
                    get() = CoroutineScopeStub()
                    """.trimIndent()
            ),
            kotlin(
                """
                package kotlinx.coroutines
                import androidx.lifecycle.CoroutineContextStub
                import kotlin.coroutines.CoroutineContext
                public interface Job : CoroutineContext
                public interface CoroutineScope {
                    public val coroutineContext: CoroutineContext
                }
                class JobStub: Job {
                    override fun plus(context: CoroutineContext): CoroutineContext = CoroutineContextStub()
                }
                public fun CoroutineScope.launch(
                    context: CoroutineContext,
                    block: suspend CoroutineScope.() -> Unit
                ): Job = JobStub()
                public fun SupervisorJob(parent: Job? = null) : Job = JobStub()
                public fun Job(parent: Job? = null): Job = JobStub()
                object Dispatchers {
                    val IO = CoroutineContextStub()
                }
                public object NonCancellable : Job {
                    override fun plus(context: CoroutineContext): CoroutineContext = CoroutineContextStub()
                }
                    """.trimIndent()
            ),
        )

            .testModes(TestMode.DEFAULT)
            .run()
            .expect(
                """
                src/ru/otus/homework/linthomework/jobinbuilderusage/JobInBuilderTestCase.kt:13: Warning: It is not allowed to use Job or SupervisorJob in a coroutine constructor [JobInBuilderUsage]
                        viewModelScope.launch(SupervisorJob() + Dispatchers.IO) {
                                              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/ru/otus/homework/linthomework/jobinbuilderusage/JobInBuilderTestCase.kt:18: Warning: It is not allowed to use Job or SupervisorJob in a coroutine constructor [JobInBuilderUsage]
                        viewModelScope.launch(Job()) {
                                              ~~~~~
                src/ru/otus/homework/linthomework/jobinbuilderusage/JobInBuilderTestCase.kt:23: Warning: It is not allowed to use Job or SupervisorJob in a coroutine constructor [JobInBuilderUsage]
                        viewModelScope.launch(job) {
                                              ~~~
                src/ru/otus/homework/linthomework/jobinbuilderusage/JobInBuilderTestCase.kt:28: Warning: It is not allowed to use Job or SupervisorJob in a coroutine constructor [JobInBuilderUsage]
                        viewModelScope.launch(NonCancellable) {
                                              ~~~~~~~~~~~~~~
                src/ru/otus/homework/linthomework/jobinbuilderusage/JobInBuilderTestCase.kt:33: Warning: It is not allowed to use Job or SupervisorJob in a coroutine constructor [JobInBuilderUsage]
                        viewModelScope.launch(SupervisorJob()) {
                                              ~~~~~~~~~~~~~~~
                0 errors, 5 warnings
            """.trimIndent()
            )
    }
}