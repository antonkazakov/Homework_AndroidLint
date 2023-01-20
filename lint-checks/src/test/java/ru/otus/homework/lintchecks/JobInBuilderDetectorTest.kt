package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Assert.*
import org.junit.Test

@Suppress("UnstableApiUsage")
internal class JobInBuilderDetectorTest {

    private val lintTask = lint().allowMissingSdk().issues(JobInBuilderDetector.ISSUE)

    @Test
    fun `should detect Job`() {
        lintTask.files(
            kotlin("""
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
            ), kotlinxCoroutines, kotlinCoroutines, androidxLifecycle
        )

            .testModes(TestMode.DEFAULT)
            .run()
            .expect("""
                src/ru/otus/homework/linthomework/jobinbuilderusage/JobInBuilderTestCase.kt:16: Warning: Job or SupervisorJob use in coroutine builder is not allowed. [JobInBuilderUsage]
                        viewModelScope.launch(SupervisorJob() + Dispatchers.IO) {
                                              ~~~~~~~~~~~~~~~
                src/ru/otus/homework/linthomework/jobinbuilderusage/JobInBuilderTestCase.kt:22: Warning: Job or SupervisorJob use in coroutine builder is not allowed. [JobInBuilderUsage]
                        viewModelScope.launch(Job()) {
                                              ~~~~~
                src/ru/otus/homework/linthomework/jobinbuilderusage/JobInBuilderTestCase.kt:28: Warning: Job or SupervisorJob use in coroutine builder is not allowed. [JobInBuilderUsage]
                        viewModelScope.launch(job) {
                                              ~~~
                src/ru/otus/homework/linthomework/jobinbuilderusage/JobInBuilderTestCase.kt:33: Warning: Job or SupervisorJob use in coroutine builder is not allowed. [JobInBuilderUsage]
                        viewModelScope.launch(NonCancellable) {
                                              ~~~~~~~~~~~~~~
                src/ru/otus/homework/linthomework/jobinbuilderusage/JobInBuilderTestCase.kt:39: Warning: Job or SupervisorJob use in coroutine builder is not allowed. [JobInBuilderUsage]
                        viewModelScope.launch(SupervisorJob()) {
                                              ~~~~~~~~~~~~~~~
                0 errors, 5 warnings
            """.trimIndent())
    }

}
