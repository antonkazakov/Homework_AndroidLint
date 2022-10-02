package ru.otus.homework.detectors

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test
import ru.otus.homework.detectors.stubs.coroutines
import ru.otus.homework.detectors.stubs.coroutinesKotlinX
import ru.otus.homework.detectors.stubs.viewModelFile
import ru.otus.homework.detectors.stubs.viewModelScope
import ru.otus.homework.lintchecks.detectors.JobInBuilderUsageDetector

class JobInBuilderUsageDetectorTest {

    private val testLint = TestLintTask
        .lint()
        .allowMissingSdk()
        .issues(JobInBuilderUsageDetector.ISSUE)

    private val testCode = kotlin(
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
                        delay(1000)
                        println("Hello World")
                    }
                }
    
                fun case2() {
                    viewModelScope.launch(Job()) {
                        delay(1000)
                        println("Hello World")
                    }
                }
    
                fun case3() {
                    viewModelScope.launch(job) {
                        delay(1000)
                        println("Hello World")
                    }
                }
    
                fun case4() {
                    viewModelScope.launch(NonCancellable) {
                        delay(1000)
                        println("Hello World")
                    }
                }
    
                fun case5() {
                    viewModelScope.launch {
                        launch(NonCancellable) {
                            delay(1000)
                            println("Hello World")
                        }
                    }
                }
            }
        """.trimIndent()).indented()

    @Test
    fun test1() {
        testLint
            .files(
            testCode, viewModelFile, viewModelScope, coroutinesKotlinX, coroutines
        )
            .run()
            .expect("")
    }
}