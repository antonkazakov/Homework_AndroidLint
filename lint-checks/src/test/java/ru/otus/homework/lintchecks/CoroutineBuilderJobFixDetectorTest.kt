package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test
import ru.otus.homework.lintchecks.CommonStubs.coroutinesStub
import ru.otus.homework.lintchecks.CommonStubs.viewModelStub

class CoroutineBuilderJobFixDetectorTest {

    private val lintTask = TestLintTask.lint()
        .allowMissingSdk()
        .issues(CoroutineBuilderJobDetector.ISSUE)

    @Test
    fun `fix supervisor job usage`() {
        val file = LintDetectorTest.kotlin(
            """
                import androidx.lifecycle.ViewModel
                import androidx.lifecycle.viewModelScope
                import kotlinx.coroutines.SupervisorJob
                import kotlinx.coroutines.launch
                
                class JobInBuilderTestCase: ViewModel() {
                
                    fun test() {
                        viewModelScope.launch(SupervisorJob()) {
                            println("Hello World")
                        }
                    }
                }
            """.trimIndent()
        )

        val expectedFixDiffs =
            """
                Fix for src/JobInBuilderTestCase.kt line 9: На viewModelScope внутри ViewModel удалить SupervisorJob:
                @@ -9 +9
                -         viewModelScope.launch(SupervisorJob()) {
                +         viewModelScope.launch {
            """.trimIndent()

        lintTask.files(file, coroutinesStub, viewModelStub)
            .run()
            .expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `fix non-cancellable usage`() {
        val file = LintDetectorTest.kotlin(
            """
                import androidx.lifecycle.ViewModel
                import androidx.lifecycle.viewModelScope
                import kotlinx.coroutines.launch
                
                class JobInBuilderTestCase: ViewModel() {
                
                    fun test() {
                        viewModelScope.launch(NonCancellable) {
                            println("Hello World")
                        }
                    }
                }
            """.trimIndent()
        )

        val expectedFixDiffs =
            """
                Fix for src/JobInBuilderTestCase.kt line 8: На viewModelScope внутри ViewModel заменить вызов launch/async на withContext:
                @@ -8 +8
                -         viewModelScope.launch(NonCancellable) {
                +         withContext(NonCancellable) {
            """.trimIndent()

        lintTask.files(file, coroutinesStub, viewModelStub)
            .run()
            .expect(expectedFixDiffs)
    }
}