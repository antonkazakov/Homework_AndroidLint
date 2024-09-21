import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test
import ru.otus.homework.lintchecks.GlobalScopeUsageDetector

class GlobalScopeUsageDetectorTest {
    private val lintTask = lint()
        .allowMissingSdk()
        .issues(GlobalScopeUsageDetector.ISSUE)

    @Test
    fun `detect global scope usage`() {
        lintTask
            .files(
                kotlin(
                    """
                class GlobalScopeTestCase(private val scope: CoroutineScope) : ViewModel() {
                    
                    fun case1() {
                        GlobalScope.launch {
                            delay(1000)
                            println("Hello World")
                        }
                        GlobalScope.actor<String> {
                            delay(1000)
                            println("Hello World")
                        }
                    }
                }
            """
                )
            )
            .run()
            .expectWarningCount(2)
    }

    @Test
    fun `detect global scope inside other scope`() {
        lintTask
            .files(
                kotlin(
                    """
                class GlobalScopeTestCase(private val scope: CoroutineScope) : ViewModel() {
                    
                    fun case2() {
                        viewModelScope.launch {
                            val deferred = GlobalScope.async {
                                delay(1000)
                                "Hello World"
                            }
                            println(deferred.await())
                        }
                    }
                }
            """
                )
            )
            .run()
            .expectWarningCount(1)
    }

    @Test
    fun `detect no warnings`() {
        lintTask
            .files(
                kotlin(
                    """
                class GlobalScopeTestCase(private val scope: CoroutineScope) : ViewModel() {
                    
                    fun case3() {
                        scope.launch {
                            delay(1000)
                            println("Hello World")
                        }
                    }
                }
            """
                )
            )
            .run()
            .expectClean()
    }
}