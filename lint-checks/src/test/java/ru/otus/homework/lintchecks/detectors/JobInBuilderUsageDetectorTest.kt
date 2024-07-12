package ru.otus.homework.lintchecks.detectors

import com.android.tools.lint.checks.infrastructure.TestFiles
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test

@Suppress("UnstableApiUsage")
class JobInBuilderUsageDetectorTest {


    @Test
    fun checkJob() {
        TestLintTask.lint()
            .files(
                Stubs.viewModelStub,
                Stubs.viewModelExtensionsStub,
                Stubs.coroutineStub,
                testFile
            )
            .allowMissingSdk()
            .detector(JobInBuilderUsageDetector())
            .issues(JobInBuilderUsageDetector.ISSUE)
            .run()
            .expectWarningCount(4)
            .expect(
                """src/ru/otus/homework/linthomework/jobinbuilderusage/JobInBuilderTestCase.kt:17: Warning: Найден экземпляр Job/SupervisorJob в корутин билдер [JobInBuilderUsage]
                        viewModelScope.launch(SupervisorJob() + Dispatchers.IO) {
                                              ~~~~~~~~~~~~~~~
src/ru/otus/homework/linthomework/jobinbuilderusage/JobInBuilderTestCase.kt:24: Warning: Найден экземпляр Job/SupervisorJob в корутин билдер [JobInBuilderUsage]
                        viewModelScope.launch(Job()) {
                                              ~~~~~
src/ru/otus/homework/linthomework/jobinbuilderusage/JobInBuilderTestCase.kt:31: Warning: Найден экземпляр Job/SupervisorJob в корутин билдер [JobInBuilderUsage]
                        viewModelScope.launch(job) {
                                              ~~~
src/ru/otus/homework/linthomework/jobinbuilderusage/JobInBuilderTestCase.kt:38: Warning: Найден экземпляр Job/SupervisorJob в корутин билдер [JobInBuilderUsage]
                        viewModelScope.launch(NonCancellable) {
                                              ~~~~~~~~~~~~~~
0 errors, 4 warnings""".trimIndent()
            )
    }

    private val testFile = TestFiles.kotlin(
        """
        package ru.otus.homework.linthomework.jobinbuilderusage

                        import androidx.lifecycle.ViewModel
                        import androidx.lifecycle.viewModelScope
                        import kotlinx.coroutines.Dispatchers
                        import kotlinx.coroutines.Job
                        import kotlinx.coroutines.NonCancellable
                        import kotlinx.coroutines.SupervisorJob
                        import kotlinx.coroutines.delay
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
                        }
            """.trimIndent()
    ).indented()
}