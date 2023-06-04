package kz.flyingv.shutapplints

import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import ru.otus.homework.lintchecks.GlobalScopeDetector
import ru.otus.homework.linttests.AndroidSdkLintDetectorTest

class GlobalScopeTest: AndroidSdkLintDetectorTest() {

    override fun getDetector(): Detector = GlobalScopeDetector()

    override fun getIssues(): MutableList<Issue> = mutableListOf(GlobalScopeDetector.ISSUE)

    @Test
    fun testScope(){

        val kotlinFile = kotlin(
            """
                package kz.flyingv.shutapplints
                
                import kotlinx.coroutines.GlobalScope
                import kotlinx.coroutines.launch
                
                class MyClass {

                    fun method1() {
                        GlobalScope.launch {  }
                    }
                }

            """
        ).indented()

        val lintResult = lint()
            .files(kotlinFile)
            .issues(GlobalScopeDetector.ISSUE)
            .allowCompilationErrors()
            .run()

        lintResult
            .expectErrorCount(1)

    }

}