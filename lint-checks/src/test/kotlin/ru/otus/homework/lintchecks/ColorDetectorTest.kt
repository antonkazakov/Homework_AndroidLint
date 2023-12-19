package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.TestFiles
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test

internal class ColorDetectorTest {

    private val lintTask = TestLintTask
        .lint()
        .allowMissingSdk()
        .issues(ColorDetector.ISSUE)

    @Test
    fun `Testing colors`() {
        lintTask
            .detector(ColorDetector())
            .files(
                TestFiles.xml(
                    "res/values/colors.xml",
                    """
                    <?xml version="1.0" encoding="utf-8"?>
                    <resources>
                        <color name="color_1">#FF111111</color>
                        <color name="color_2">#FF2222</color>
                        <color name="color_3">#33033333</color>
                    </resources>
                """.trimIndent()
                ).indented(),
                TestFiles.xml(
                    "res/layout/incorrect_color_usages_layout.xml",
                    """
                        <?xml version="1.0" encoding="utf-8"?>
                            <LinerLayout
                                xmlns:android="http://schemas.android.com/apk/res/android"
                                xmlns:app="http://schemas.android.com/apk/res-auto"
                                android:orientation="vertical"
                                android:layout_width="match_parent"
                                android:layout_height="match_parent">
                            
                                <View
                                    android:layout_width="80dp"
                                    android:layout_height="80dp"
                                    android:background="#FF2222"/>
                            
                                <View
                                    android:layout_width="80dp"
                                    android:layout_height="80dp"
                                    android:background="#00bcd4"/>
                            
                                <View
                                    android:layout_width="80dp"
                                    android:layout_height="80dp"
                                    android:background="@android:color/holo_blue_dark"/>
                            
                                <View
                                    android:layout_width="80dp"
                                    android:layout_height="80dp"
                                    android:background="@color/color_3"/>
                            
                                <View
                                    android:layout_width="80dp"
                                    android:layout_height="80dp"
                                    android:background="@color/selector"/>
                            
                                <View
                                    android:layout_width="80dp"
                                    android:layout_height="80dp"
                                    android:background="@drawable/ic_baseline_adb_24"
                                    android:backgroundTint="@color/color_1"/>
                                
                            </LinerLayout>
                    """.trimIndent()
                ).indented()
            )
            .run()
            .checkFix(
                null,
                TestFiles.xml(
                    "res/layout/incorrect_color_usages_layout.xml",
                    """
                        <?xml version="1.0" encoding="utf-8"?>
                            <LinerLayout
                                xmlns:android="http://schemas.android.com/apk/res/android"
                                xmlns:app="http://schemas.android.com/apk/res-auto"
                                android:orientation="vertical"
                                android:layout_width="match_parent"
                                android:layout_height="match_parent">
                            
                                <View
                                    android:layout_width="80dp"
                                    android:layout_height="80dp"
                                    android:background="@color/color_2"/>
                            
                                <View
                                    android:layout_width="80dp"
                                    android:layout_height="80dp"
                                    android:background="#00bcd4"/>
                            
                                <View
                                    android:layout_width="80dp"
                                    android:layout_height="80dp"
                                    android:background="@android:color/holo_blue_dark"/>
                            
                                <View
                                    android:layout_width="80dp"
                                    android:layout_height="80dp"
                                    android:background="@color/color_3"/>
                            
                                <View
                                    android:layout_width="80dp"
                                    android:layout_height="80dp"
                                    android:background="@color/selector"/>
                            
                                <View
                                    android:layout_width="80dp"
                                    android:layout_height="80dp"
                                    android:background="@drawable/ic_baseline_adb_24"
                                    android:backgroundTint="@color/color_1"/>
                                
                            </LinerLayout>
                    """.trimIndent()
                ).indented()
            ).expect(
                """
                    res/layout/incorrect_color_usages_layout.xml:12: Warning: Usage the color arbitrary isn`t allowed [ArbitraryColorsUsage]
                                android:background="#FF2222"/>
                                                    ~~~~~~~
                    res/layout/incorrect_color_usages_layout.xml:17: Warning: Usage the color arbitrary isn`t allowed [ArbitraryColorsUsage]
                                android:background="#00bcd4"/>
                                                    ~~~~~~~
                    0 errors, 2 warnings
                """.trimIndent()
            )
    }
}
