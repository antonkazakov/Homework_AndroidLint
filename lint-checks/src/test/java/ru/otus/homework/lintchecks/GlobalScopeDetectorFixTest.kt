package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test
import ru.otus.homework.lintchecks.CommonStubs.coroutinesStub
import ru.otus.homework.lintchecks.CommonStubs.viewModelStub

class GlobalScopeDetectorFixTest {

    private val lintTask = TestLintTask.lint()
        .allowMissingSdk()
        .issues(GlobalScopeDetector.ISSUE)

    /***
     *  Тесты не проходят
     *  Предполагаю, что неправильно пишу gradle-stub
     */

    @Test
    fun `check replace with view model scope using build gradle`() {
        val file = LintDetectorTest.kotlin(
            """
                import androidx.lifecycle.ViewModel
                import kotlinx.coroutines.GlobalScope
                import kotlinx.coroutines.launch
                
                class GlobalScopeTestCase : ViewModel() {
                
                    fun callGlobalScope() {
                        GlobalScope.launch {}
                    }
                }
            """.trimIndent()
        )
        val expected =
            """
                Fix for src/GlobalScopeTestCase.kt line 6: Заменить GlobalScope на viewModelScope:
                @@ -8 +8
                -         GlobalScope.launch {
                +         viewModelScope.launch {
            """.trimIndent()
        checkWithGradle(file, buildGradleStub, expected)
    }

    @Test
    fun `check replace with view model scope using build gradle kts`() {
        val file = LintDetectorTest.kotlin(
            """
                import androidx.lifecycle.ViewModel
                import kotlinx.coroutines.GlobalScope
                import kotlinx.coroutines.launch
                
                class GlobalScopeTestCase : ViewModel() {
                
                    fun callGlobalScope() {
                        GlobalScope.launch {}
                    }
                }
            """.trimIndent()
        )
        val expected =
            """
                Fix for src/GlobalScopeTestCase.kt line 6: Заменить GlobalScope на viewModelScope:
                @@ -8 +8
                -         GlobalScope.launch {
                +         viewModelScope.launch {
            """.trimIndent()
        checkWithGradle(file, buildGradleKtsStub, expected)
    }

    @Test
    fun `check replace with lifecycle scope using build gradle`() {
        val file = LintDetectorTest.kotlin(
            """
                import kotlinx.coroutines.GlobalScope
                import kotlinx.coroutines.launch
                
                class GlobalScopeTestCase : Fragment() {
                
                    fun callGlobalScope() {
                        GlobalScope.launch {}
                    }
                }
            """.trimIndent()
        )
        val expected =
            """
                Fix for src/GlobalScopeTestCase.kt line 6: Заменить GlobalScope на lifecycleScope:
                @@ -8 +8
                -         GlobalScope.launch {
                +         lifecycleScope.launch {
            """.trimIndent()
        checkWithGradle(file, buildGradleStub, expected)
    }

    @Test
    fun `check replace with lifecycle scope using build gradle kts`() {
        val file = LintDetectorTest.kotlin(
            """
                import kotlinx.coroutines.GlobalScope
                import kotlinx.coroutines.launch
                
                class GlobalScopeTestCase {
                
                    fun callGlobalScope() {
                        GlobalScope.launch {}
                    }
                }
            """.trimIndent()
        )
        val expected =
            """
                Fix for src/GlobalScopeTestCase.kt line 6: Заменить GlobalScope на lifecycleScope:
                @@ -8 +8
                -         GlobalScope.launch {
                +         lifecycleScope.launch {
            """.trimIndent()
        checkWithGradle(file, buildGradleKtsStub, expected)
    }

    private fun checkWithGradle(file: TestFile, gradleFile: TestFile, expected: String) {
        lintTask.files(file, coroutinesStub, viewModelStub, gradleFile)
            .run()
            .expectFixDiffs(expected)
    }

    private val buildGradleStub = LintDetectorTest.gradle(
        """
            dependencies {
                implementation "androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2"
                implementation "androidx.lifecycle:lifecycle-runtime-ktx:2.6.2"
            }
        """.trimIndent()
    )

    private val buildGradleKtsStub = LintDetectorTest.kts(
        """
            dependencies {
                implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
                implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
            }
        """.trimIndent()
    )
}