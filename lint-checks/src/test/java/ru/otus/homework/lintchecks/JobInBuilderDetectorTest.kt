package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestFiles
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test
import ru.otus.homework.lintchecks.detector.JobInBuilderDetector

@Suppress("UnstableApiUsage")
class JobInBuilderUsageDetectorTest {

    @Test
    fun checkJob() {
        TestLintTask.lint()
            .files(
                viewModelStub,
                viewModelExtensionsStub,
                coroutineStub,
                testFile
            )
            .allowMissingSdk()
            .detector(JobInBuilderDetector())
            .issues(JobInBuilderDetector.ISSUE)
            .run()
            .expectWarningCount(2)
    }

    private val testFile = TestFiles.kotlin(
        """
        package ru.otus.homework.linthomework.jobinbuilderusage

                        import androidx.lifecycle.ViewModel
                        import androidx.lifecycle.viewModelScope
                        import kotlinx.coroutines.Job
                        import kotlinx.coroutines.SupervisorJob
                        import kotlinx.coroutines.launch

                        class JobInBuilderTestCase() : ViewModel() {
                            fun case1() {
                                viewModelScope.launch(SupervisorJob()) {        
                                    println("case1()")
                                }
                            }

                            fun case2() {
                                viewModelScope.launch(Job()) {
                                    println("case2()")
                                }
                            }
                        }
            """.trimIndent()
    ).indented()

    companion object {
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
}