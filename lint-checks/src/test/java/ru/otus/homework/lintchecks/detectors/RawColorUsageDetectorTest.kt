package ru.otus.homework.lintchecks.detectors

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.xml
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

@Suppress("UnstableApiUsage")
class RawColorUsageDetectorTest {
    @Test
    fun testExistsColor() {
        lint()
            .files(
                xml(
                    "res/layout/test_layout.xml",
                    """
                        <selector xmlns:android="http://schemas.android.com/apk/res/android">
                            <item android:color="#5C6BC0" android:state_enabled="true" />
                            <item android:color="#C5CAE9" />
                        </selector>
                    """.trimIndent()
                )
            )
            .allowMissingSdk()
            .allowCompilationErrors()
            .detector(RawColorUsageDetector())
            .issues(RawColorUsageDetector.ISSUE)
            .run()
            .expect(
                """res/layout/test_layout.xml:2: Warning: Цвет не из дизайн системы [RawColorUsage]
    <item android:color="#5C6BC0" android:state_enabled="true" />
          ~~~~~~~~~~~~~~~~~~~~~~~
res/layout/test_layout.xml:3: Warning: Цвет не из дизайн системы [RawColorUsage]
    <item android:color="#C5CAE9" />
          ~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 2 warnings"""
            )
    }
}