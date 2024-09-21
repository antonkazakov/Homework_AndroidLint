import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test
import ru.otus.homework.lintchecks.JobInBuilderUsageDetector

@Suppress("UnstableApiUsage")
internal class JobInBuilderDetectorTest {

    private val lintTask = lint()
        .allowMissingSdk()
        .issues(JobInBuilderUsageDetector.ISSUE)

    @Test
    fun `detect job in builders`() {
        lintTask.files(
            kotlin(
                """
                    package checks
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
            kotlinxCoroutines,
            kotlinCoroutines,
            androidxLifecycle
        )
            .issues(JobInBuilderUsageDetector.ISSUE)
            .run()
            .expect(
                """
                src/checks/JobInBuilderTestCase.kt:13: Warning: 
                            Passing a Job instance directly to launch or async is usually redundant or even harmful. 
                            It can break expected error handling and cancellation behavior.
                         [JobInBuilderUsage]
                        viewModelScope.launch(SupervisorJob() + Dispatchers.IO) {
                                              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/checks/JobInBuilderTestCase.kt:18: Warning: 
                            Passing a Job instance directly to launch or async is usually redundant or even harmful. 
                            It can break expected error handling and cancellation behavior.
                         [JobInBuilderUsage]
                        viewModelScope.launch(Job()) {
                                              ~~~~~
                src/checks/JobInBuilderTestCase.kt:23: Warning: 
                            Passing a Job instance directly to launch or async is usually redundant or even harmful. 
                            It can break expected error handling and cancellation behavior.
                         [JobInBuilderUsage]
                        viewModelScope.launch(job) {
                                              ~~~
                src/checks/JobInBuilderTestCase.kt:28: Warning: 
                            Passing a Job instance directly to launch or async is usually redundant or even harmful. 
                            It can break expected error handling and cancellation behavior.
                         [JobInBuilderUsage]
                        viewModelScope.launch(NonCancellable) {
                                              ~~~~~~~~~~~~~~
                src/checks/JobInBuilderTestCase.kt:33: Warning: 
                            Passing a Job instance directly to launch or async is usually redundant or even harmful. 
                            It can break expected error handling and cancellation behavior.
                         [JobInBuilderUsage]
                        viewModelScope.launch(SupervisorJob()) {
                                              ~~~~~~~~~~~~~~~
                0 errors, 5 warnings
            """.trimIndent()
            )
    }
}