package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import ru.otus.homework.lintchecks.jobbuilder.JobBuilderIssue

@Suppress("UnstableApiUsage")
class JobBuilderUsageTest {

    @Test
    fun testShow_error_if_Job_used_in_coroutine_builder() {
        lint()
            .files(
                kotlin(
                    """
                    package checks

                    import kotlinx.coroutines.CoroutineScope
                    import kotlinx.coroutines.Job
                    import kotlinx.coroutines.launch

                    class TestClass {
                        fun onCreate() {
                            val job = Job()
                            val scope = CoroutineScope(Job())
                            scope.launch(Job()) {}
                            scope.launch(context = Job(), block = {})
                            scope.launch(context = job) {}
                        }
                    }
                    """.trimIndent()
                ),
                kotlinxCoroutines
            )
            .issues(JobBuilderIssue.ISSUE)
            .run()
            .expect(
                """
                    src/checks/TestClass.kt:11: Error: Not use Job/SupervisorJob in coroutine builder [JobBuilderIssue]
                            scope.launch(Job()) {}
                                  ~~~~~~
                    src/checks/TestClass.kt:12: Error: Not use Job/SupervisorJob in coroutine builder [JobBuilderIssue]
                            scope.launch(context = Job(), block = {})
                                  ~~~~~~
                    src/checks/TestClass.kt:13: Error: Not use Job/SupervisorJob in coroutine builder [JobBuilderIssue]
                            scope.launch(context = job) {}
                                  ~~~~~~
                    3 errors, 0 warnings
                """.trimIndent()
            )
    }

    @Test
    fun testShow_error_if_SupervisorJob_used_in_viewModelScope_builder() {
        lint()
            .allowMissingSdk()
            .files(
                kotlin(
                    """
                        package checks
                        import androidx.lifecycle.ViewModel
                        import androidx.lifecycle.viewModelScope
                        import kotlinx.coroutines.SupervisorJob
                        import kotlinx.coroutines.launch
                        
                        class TestClass : ViewModel() {
                            fun onCreate() {
                                viewModelScope.launch(SupervisorJob()) {}
                            }
                        }
                    """.trimIndent()
                ),
                kotlinxCoroutines,
                androidxLifecycle
            )
            .issues(JobBuilderIssue.ISSUE)
            .testModes(TestMode.PARENTHESIZED)
            .run()
            .expect(
                """
                    src/checks/TestClass.kt:9: Error: Not use Job/SupervisorJob in coroutine builder [JobBuilderIssue]
                            viewModelScope.launch((SupervisorJob())) {}
                                           ~~~~~~
                    1 errors, 0 warnings
                """.trimIndent()
            )
    }
}
