import com.android.tools.lint.checks.infrastructure.TestFiles.java
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test
import ru.otus.homework.lintchecks.GlobalScopeDetector

class GlobalScopeDetectorTest {

    private val globalScopeStub = kotlin(
        """
            @file:OptIn(ExperimentalContracts::class)

            package kotlinx.coroutines
            @DelicateCoroutinesApi
            public object GlobalScope : CoroutineScope {
                override val coroutineContext: CoroutineContext
                    get() = EmptyCoroutineContext
            }

            public fun CoroutineScope.launch(
                context: CoroutineContext = EmptyCoroutineContext,
                start: CoroutineStart = CoroutineStart.DEFAULT,
                block: suspend CoroutineScope.() -> Unit
            ): Job {
                val newContext = newCoroutineContext(context)
                val coroutine = if (start.isLazy)
                    LazyStandaloneCoroutine(newContext, block) else
                    StandaloneCoroutine(newContext, active = true)
                coroutine.start(start, coroutine, block)
                return coroutine
            }
        """.trimIndent()
    )

    private val viewModelStub = java(
        """
            package androidx.lifecycle;
            public abstract class ViewModel {
    
            }
        """.trimIndent()
    )

    private val fragmentStub = java(
        """
            package androidx.fragment.app;
            
            public class Fragment implements ComponentCallbacks, OnCreateContextMenuListener, LifecycleOwner,
        ViewModelStoreOwner, HasDefaultViewModelProviderFactory, SavedStateRegistryOwner,
        ActivityResultCaller {
            public Fragment() {
                initLifecycle();
            }
        }
        """.trimIndent()
    )

    private val lintTask = TestLintTask.lint()
        .allowMissingSdk()
        .issues(GlobalScopeDetector.ISSUE)

    @Test
    fun globalScopeDetectorViewModelTest(){
        lintTask
            .files(
                kotlin(
                    """
                        package ru.otus.homework.linthomework.globalscopeusage
                        import kotlinx.coroutines.GlobalScope
                        import kotlinx.coroutines.launch
                        import androidx.lifecycle.ViewModel

                        class ScopeTest1: ViewModel(){
                            
                            fun globalScopeUsage(){
                                GlobalScope.launch {
                                    println("Hello World")
                                }
                            }
                        }
                    """.trimIndent()
                ), globalScopeStub, viewModelStub
            )
            .run()
            .expect("""
                src/ru/otus/homework/linthomework/globalscopeusage/ScopeTest1.kt:9: Error: Корутины, запущенные на kotlinx.coroutines.GlobalScope нужно контролировать вне скоупа класса, в котором они созданы. [GlobalScopeUsage]
        GlobalScope.launch {
        ^
1 errors, 0 warnings
            """.trimIndent())
            .expectFixDiffs("Fix for src/scopeTest/pkg/ScopeTest1.kt line 9: Replace with viewModelScope:\n" +
                "@@ -9 +9\n" +
                "-         GlobalScope.launch {\n" +
                "+         viewModelScope.launch {")
    }

    @Test
    fun globalScopeDetectorFragmentTest(){
        lintTask
            .files(
                kotlin(
                    """
                        package ru.otus.homework.linthomework.globalscopeusage
                        import kotlinx.coroutines.GlobalScope
                        import kotlinx.coroutines.launch
                        import androidx.fragment.app.Fragment

                        class ScopeTest2: Fragment(){
                            
                            fun globalScopeUsage(){
                                GlobalScope.launch {
                                    println("Hello World")
                                }
                            }
                        }
                    """.trimIndent()
                ), globalScopeStub, fragmentStub
            )
            .run()
            .expect("""
                src/scopeTest/pkg/ScopeTest2.kt:9: Error: Корутины, запущенные на kotlinx.coroutines.GlobalScope нужно контролировать вне скоупа класса, в котором они созданы. [GlobalScopeUsage]
        GlobalScope.launch {
        ^
1 errors, 0 warnings
            """.trimIndent())
            .expectFixDiffs("Fix for src/scopeTest/pkg/ScopeTest2.kt line 9: Replace with lifecycleScope:\n" +
                "@@ -9 +9\n" +
                "-         GlobalScope.launch {\n" +
                "+         lifecycleScope.launch {")
    }
}