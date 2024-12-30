package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.xml
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test
import ru.otus.homework.lintchecks.detector.RawColorDetector

@Suppress("UnstableApiUsage")
class RawColorDetectorTest {

    private val lintTask = lint().allowMissingSdk().issues(RawColorDetector.ISSUE)

    private val fileColorXml = xml(
        "res/values/colors.xml",
        """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <color name="black">#FF000000</color>
                <color name="white">#FFFFFFFF</color>
                <color name="purple_200">#FFBB86FC</color>
                <color name="purple_500">#FF6200EE</color>
                <color name="purple_700">#FF3700B3</color>
                <color name="teal_200">#FF03DAC5</color>
                <color name="teal_700">#FF018786</color>
            </resources>
        """.trimIndent()
    )

    private val fileLayoutXml = xml(
        "res/layout/incorrect_color_usages_layout.xml",
        """
            <?xml version="1.0" encoding="utf-8"?>
            <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                xmlns:app="http://schemas.android.com/apk/res-auto"
                xmlns:tools="http://schemas.android.com/tools"
                android:orientation="vertical"
                android:layout_width="match_parent"
                android:layout_height="match_parent">
            
            
                <TextView
                    android:id="@+id/textView"
                    android:textColor="@color/teal_200"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="TextView" />
            
                <TextView
                    android:id="@+id/textView2"
                    android:textColor="#FF03DAC5"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="TextView" />
            
                <LinearLayout
                    android:background="@color/teal_700"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent"
                    android:orientation="horizontal">
                    
                    </LinearLayout>
            
                <LinearLayout
                    android:background="#FF018786"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent"
                    android:orientation="horizontal">
            
                </LinearLayout>
            
                <ImageView
                    android:id="@+id/imageView"
                    android:tint="@color/black"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    tools:srcCompat="@tools:sample/avatars" />
            
                <ImageView
                    android:id="@+id/imageView2"
                    android:tint="#FF112233"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    tools:srcCompat="@tools:sample/avatars" />
            </LinearLayout>
        """.trimIndent()
    )

    @Test
    fun `should detect raw colors usage`() {
        lintTask.files(
            fileColorXml,
            fileLayoutXml,
        )
            .run()
            .expectWarningCount(3)
    }
}