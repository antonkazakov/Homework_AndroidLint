package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.xml
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test
import ru.otus.homework.lintchecks.color.ColorDetector
import ru.otus.homework.lintchecks.color.ColorIssue

@Suppress("UnstableApiUsage")
class ColorUsageTest {
    @Test
    fun check_attribute_colors_usage() {
        lint()
            .allowMissingSdk()
            .files(
                xml(
                    "res/values/colors.xml",
                    """
                    <?xml version="1.0" encoding="utf-8"?>
                    <resources>
                        <color name="purple_200">#FFBB86FC</color>
                        <color name="purple_500">#FF6200EE</color>
                        <color name="purple_700">#FF3700B3</color>
                        <color name="teal_200">#FF03DAC5</color>
                        <color name="teal_700">#FF018786</color>
                        <color name="black">#FF000000</color>
                        <color name="white">#FFFFFFFF</color>
                    </resources>
                    """.trimIndent()
                ),
                xml(
                    "res/layout/incorrect_color_usages_layout.xml",
                    """
                        <?xml version="1.0" encoding="utf-8"?>
                            <FrameLayout
                                xmlns:android="http://schemas.android.com/apk/res/android"
                                xmlns:app="http://schemas.android.com/apk/res-auto"
                                android:layout_width="match_parent"
                                android:layout_height="match_parent">
                            
                                <View
                                    android:id="@+id/case1"
                                    android:layout_width="80dp"
                                    android:layout_height="80dp"
                                    android:layout_marginTop="32dp"
                                    android:background="#FF3700B3"
                                    app:layout_constraintEnd_toEndOf="parent"
                                    app:layout_constraintStart_toStartOf="parent"
                                    app:layout_constraintTop_toTopOf="parent" />
                            
                                <View
                                    android:id="@+id/case2"
                                    android:layout_width="80dp"
                                    android:layout_height="80dp"
                                    android:layout_marginTop="16dp"
                                    android:background="#00bcd4"
                                    app:layout_constraintEnd_toEndOf="@+id/case1"
                                    app:layout_constraintStart_toStartOf="@+id/case1"
                                    app:layout_constraintTop_toBottomOf="@+id/case1" />
                            
                                <View
                                    android:id="@+id/case3"
                                    android:layout_width="80dp"
                                    android:layout_height="80dp"
                                    android:layout_marginTop="16dp"
                                    android:background="@android:color/holo_blue_dark"
                                    app:layout_constraintEnd_toEndOf="@+id/case2"
                                    app:layout_constraintStart_toStartOf="@+id/case2"
                                    app:layout_constraintTop_toBottomOf="@+id/case2" />
                            
                                <View
                                    android:id="@+id/case4"
                                    android:layout_width="80dp"
                                    android:layout_height="80dp"
                                    android:layout_marginTop="16dp"
                                    android:background="@color/teal_700"
                                    app:layout_constraintEnd_toEndOf="@+id/case3"
                                    app:layout_constraintStart_toStartOf="@+id/case3"
                                    app:layout_constraintTop_toBottomOf="@+id/case3" />
                            
                                <View
                                    android:id="@+id/case5"
                                    android:layout_width="80dp"
                                    android:layout_height="80dp"
                                    android:layout_marginTop="16dp"
                                    android:background="@color/selector"
                                    app:layout_constraintEnd_toEndOf="@+id/case4"
                                    app:layout_constraintStart_toStartOf="@+id/case4"
                                    app:layout_constraintTop_toBottomOf="@+id/case4" />
                            
                                <View
                                    android:id="@+id/case6"
                                    android:layout_width="80dp"
                                    android:layout_height="80dp"
                                    android:layout_marginTop="16dp"
                                    android:background="@drawable/ic_baseline_adb_24"
                                    android:backgroundTint="@color/purple_200"
                                    android:backgroundTintMode="add"
                                    app:layout_constraintEnd_toEndOf="@+id/case5"
                                    app:layout_constraintStart_toStartOf="@+id/case5"
                                    app:layout_constraintTop_toBottomOf="@+id/case5" />
                            
                            </FrameLayout>
                    """.trimIndent()
                )
            )
            .detector(ColorDetector())
            .issues(ColorIssue.ISSUE)
            .run()
            .expect(
                """
                    res/layout/incorrect_color_usages_layout.xml:23: Error: Use only predefined colors [ColorIssue]
                                android:background="#00bcd4"
                                                    ~~~~~~~
                    res/layout/incorrect_color_usages_layout.xml:33: Error: Use only predefined colors [ColorIssue]
                                android:background="@android:color/holo_blue_dark"
                                                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    res/layout/incorrect_color_usages_layout.xml:53: Error: Use only predefined colors [ColorIssue]
                                android:background="@color/selector"
                                                    ~~~~~~~~~~~~~~~
                    3 errors, 0 warnings
                """.trimIndent()
            )
    }
}
