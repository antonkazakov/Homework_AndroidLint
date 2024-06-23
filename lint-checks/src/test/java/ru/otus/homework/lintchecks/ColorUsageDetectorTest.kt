@file:Suppress("UnstableApiUsage")

package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.xml
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ColorUsageDetectorTest {

    @Test
    fun testExistsColor() {
        lint()
            .files(
                xml(
                    "res/values/colors.xml",
                    """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="purple_700">#FF3700B3</color>
</resources>"""
                ),
                xml(
                    "res/layout/test_layout.xml",
                    """<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#FF3700B3" />"""
                )
            )
            .allowMissingSdk()
            .testModes(TestMode.DEFAULT)
            .detector(ColorUsageDetector())
            .issues(ColorUsageDetector.ISSUE)
            .run()
            .expect("""res/layout/test_layout.xml:5: Warning: Используйте цвет @color/purple_700 [RawColorUsage]
    android:background="#FF3700B3" />
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warnings""")
    }
}
