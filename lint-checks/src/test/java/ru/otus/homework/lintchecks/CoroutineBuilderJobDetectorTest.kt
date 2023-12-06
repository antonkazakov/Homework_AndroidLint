package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test
import ru.otus.homework.lintchecks.CommonStubs.coroutinesStub
import ru.otus.homework.lintchecks.CommonStubs.viewModelStub

class CoroutineBuilderJobDetectorTest {

    private val lintTask = TestLintTask.lint()
        .allowMissingSdk()
        .issues(CoroutineBuilderJobDetector.ISSUE)

    @Test
    fun `check supervisor job usage case 1`() {
        val file = LintDetectorTest.kotlin(
            """
                import androidx.lifecycle.ViewModel
                import androidx.lifecycle.viewModelScope
                import kotlinx.coroutines.Dispatchers
                import kotlinx.coroutines.SupervisorJob
                import kotlinx.coroutines.delay
                import kotlinx.coroutines.launch
                
                class JobInBuilderTestCase: ViewModel() {
                
                    fun case1() {
                        viewModelScope.launch(SupervisorJob() + Dispatchers.IO) {
                            delay(1000)
                            println("Hello World")
                        }
                    }
                }
            """.trimIndent()
        )
        val expected =
            """
                src/JobInBuilderTestCase.kt:11: Warning: Не используйте Job/SupervisorJob внутри корутин-билдеров [JobInBuilderUsage]
                        viewModelScope.launch(SupervisorJob() + Dispatchers.IO) {
                        ^
                0 errors, 1 warnings
            """.trimIndent()
        check(file, expected)
    }

    @Test
    fun `check job usage case 2`() {
        val file = LintDetectorTest.kotlin(
            """
                import androidx.lifecycle.ViewModel
                import androidx.lifecycle.viewModelScope
                import kotlinx.coroutines.Job
                import kotlinx.coroutines.delay
                import kotlinx.coroutines.launch
                
                class JobInBuilderTestCase: ViewModel() {
                
                    fun case2() {
                        viewModelScope.launch(Job()) {
                            delay(1000)
                            println("Hello World")
                        }
                    }
                }
            """.trimIndent()
        )
        val expected =
            """
                src/JobInBuilderTestCase.kt:10: Warning: Не используйте Job/SupervisorJob внутри корутин-билдеров [JobInBuilderUsage]
                        viewModelScope.launch(Job()) {
                        ^
                0 errors, 1 warnings
            """.trimIndent()
        check(file, expected)
    }

    @Test
    fun `check job usage case 3`() {
        val file = LintDetectorTest.kotlin(
            """
                import androidx.lifecycle.ViewModel
                import androidx.lifecycle.viewModelScope
                import kotlinx.coroutines.Job
                import kotlinx.coroutines.delay
                import kotlinx.coroutines.launch
                
                class JobInBuilderTestCase(private val job: Job): ViewModel() {
                
                    fun case3() {
                        viewModelScope.launch(job) {
                            delay(1000)
                            println("Hello World")
                        }
                    }
                }
            """.trimIndent()
        )
        lintTask.files(file, coroutinesStub, viewModelStub)
            .run()
            .expectClean()
    }

    private fun check(file: TestFile, expected: String) {
        lintTask.files(file, coroutinesStub, viewModelStub)
            .run()
            .expect(expected)
    }
}