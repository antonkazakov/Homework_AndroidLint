package ru.otus.homework.lintchecks.rules

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestLintTask
import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import ru.otus.homework.lintchecks.Mock

@Suppress("UnstableApiUsage")
class JobInBuilderUsageRuleTest {

    @Test
    fun detektSupervisorJobInLaunchArgs() {
        TestLintTask.lint()
            .allowMissingSdk()
            .files(
                LintDetectorTest.kotlin(
                    """
                    package ru.otus.homework.linthomework.jobinbuilderusage
                                        
                    import androidx.lifecycle.ViewModel
                    import androidx.lifecycle.viewModelScope
                    import kotlinx.coroutines.Dispatchers
                    import kotlinx.coroutines.Job
                    import kotlinx.coroutines.SupervisorJob
                    import kotlinx.coroutines.delay
                    import kotlinx.coroutines.launch
                    
                    class JobInBuilderTestCase(private val job: Job) : ViewModel() {
                    
                        fun case1() {
                            viewModelScope.launch(SupervisorJob() + Dispatchers.IO) {
                                delay(1000)
                                println("Hello World")
                            }
                        }
                    }
                """.trimIndent()
                ), Mock.kotlinxCoroutines, Mock.kotlinCoroutines, Mock.androidxLifecycle
            )
            .issues(JobInBuilderUsageRule.ISSUE)
            .testModes(TestMode.DEFAULT)
            .run()
            .expectErrorCount(1)
    }

    @Test
    fun detektJobNewInstanceInLaunchArgs() {
        TestLintTask.lint()
            .allowMissingSdk()
            .files(
                LintDetectorTest.kotlin(
                    """
                    package ru.otus.homework.linthomework.jobinbuilderusage
                                        
                    import androidx.lifecycle.ViewModel
                    import androidx.lifecycle.viewModelScope
                    import kotlinx.coroutines.Job
                    import kotlinx.coroutines.delay
                    import kotlinx.coroutines.launch
                    
                    class JobInBuilderTestCase(private val job: Job) : ViewModel() {
                    
                        fun case2() {
                            viewModelScope.launch(Job()) {
                                delay(1000)
                                println("Hello World")
                            }
                        }
                    }
                """.trimIndent()
                ), Mock.kotlinxCoroutines, Mock.kotlinCoroutines, Mock.androidxLifecycle
            )
            .issues(JobInBuilderUsageRule.ISSUE)
            .run()
            .expectErrorCount(1)
    }

    @Test
    fun detektJobInLaunchArgs() {
        TestLintTask.lint()
            .allowMissingSdk()
            .files(
                LintDetectorTest.kotlin(
                    """
                    package ru.otus.homework.linthomework.jobinbuilderusage
                                        
                    import androidx.lifecycle.ViewModel
                    import androidx.lifecycle.viewModelScope
                    import kotlinx.coroutines.Job
                    import kotlinx.coroutines.delay
                    import kotlinx.coroutines.launch
                    
                    class JobInBuilderTestCase(private val job: Job) : ViewModel() {
                    
                        fun case3() {
                            viewModelScope.launch(job) {
                                delay(1000)
                                println("Hello World")
                            }
                        }
                    }
                """.trimIndent()
                ), Mock.kotlinxCoroutines, Mock.kotlinCoroutines, Mock.androidxLifecycle
            )
            .issues(JobInBuilderUsageRule.ISSUE)
            .run()
            .expectErrorCount(1)
    }

    @Test
    fun detektNonCancellableInLaunchArgs() {
        TestLintTask.lint()
            .allowMissingSdk()
            .files(
                LintDetectorTest.kotlin(
                    """
                    package ru.otus.homework.linthomework.jobinbuilderusage
                                        
                    import androidx.lifecycle.ViewModel
                    import androidx.lifecycle.viewModelScope
                    import kotlinx.coroutines.Job
                    import kotlinx.coroutines.NonCancellable
                    import kotlinx.coroutines.delay
                    import kotlinx.coroutines.launch
                    
                    class JobInBuilderTestCase(private val job: Job) : ViewModel() {
                    
                        fun case4() {
                            viewModelScope.launch(NonCancellable) {
                                delay(1000)
                                println("Hello World")
                            }
                        }
                    }
                """.trimIndent()
                ), Mock.kotlinxCoroutines, Mock.kotlinCoroutines, Mock.androidxLifecycle
            )
            .issues(JobInBuilderUsageRule.ISSUE)
            .run()
            .expectErrorCount(1)
    }
}

