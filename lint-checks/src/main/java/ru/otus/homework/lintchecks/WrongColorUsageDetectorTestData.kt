package ru.otus.homework.lintchecks

data class XmlTestSample(
    val xmlFilePath : String,
    val xmlSource: String,
    val expectedResult: String
)

private const val LAYOUT_FILE_PATH = "res/layout/test_layout.xml"
private const val SELECTOR_FILE_PATH = "res/layout/test_selector.xml"
private const val VECTOR_FILE_PATH = "res/layout/ic_baseline_adb_24.xml"

val incidentAARRGGBB = XmlTestSample(
    xmlFilePath = LAYOUT_FILE_PATH,
    xmlSource = """
            <?xml version="1.0" encoding="utf-8"?>
                <androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
                xmlns:app="http://schemas.android.com/apk/res-auto"
                android:layout_width="match_parent"
                android:layout_height="match_parent">
                
                <View
                android:id="@+id/case1"
                android:layout_width="80dp"
                android:layout_height="80dp"
                android:layout_marginTop="32dp"
                android:background="#FF000000"
                app:layout_constraintEnd_toEndOf="parent"
                app:layout_constraintStart_toStartOf="parent"
                app:layout_constraintTop_toTopOf="parent" />
                
            </androidx.constraintlayout.widget.ConstraintLayout>
                """.trimIndent(),
    expectedResult = """res/layout/test_layout.xml:12: Warning: Should use colors only from palette [WrongColorUsage]
    android:background="#FF000000"
                        ~~~~~~~~~
0 errors, 1 warnings""".trimIndent()
)

val incidentRRGGBB = XmlTestSample(
    xmlFilePath = LAYOUT_FILE_PATH,
    xmlSource = """
            <?xml version="1.0" encoding="utf-8"?>
                <androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
                xmlns:app="http://schemas.android.com/apk/res-auto"
                android:layout_width="match_parent"
                android:layout_height="match_parent">
                
                <View
                android:id="@+id/case1"
                android:layout_width="80dp"
                android:layout_height="80dp"
                android:layout_marginTop="32dp"
                android:background="#220033"
                app:layout_constraintEnd_toEndOf="parent"
                app:layout_constraintStart_toStartOf="parent"
                app:layout_constraintTop_toTopOf="parent" />
                
            </androidx.constraintlayout.widget.ConstraintLayout>
                """.trimIndent(),
    expectedResult = """res/layout/test_layout.xml:12: Warning: Should use colors only from palette [WrongColorUsage]
    android:background="#220033"
                        ~~~~~~~
0 errors, 1 warnings""".trimIndent()
)


val incidentARGB = XmlTestSample(
    xmlFilePath = LAYOUT_FILE_PATH,
    xmlSource = """
            <?xml version="1.0" encoding="utf-8"?>
                <androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
                xmlns:app="http://schemas.android.com/apk/res-auto"
                android:layout_width="match_parent"
                android:layout_height="match_parent">
                
                <View
                android:id="@+id/case1"
                android:layout_width="80dp"
                android:layout_height="80dp"
                android:layout_marginTop="32dp"
                android:background="#F000"
                app:layout_constraintEnd_toEndOf="parent"
                app:layout_constraintStart_toStartOf="parent"
                app:layout_constraintTop_toTopOf="parent" />
                
            </androidx.constraintlayout.widget.ConstraintLayout>
                """.trimIndent(),
    expectedResult = """res/layout/test_layout.xml:12: Warning: Should use colors only from palette [WrongColorUsage]
    android:background="#F000"
                        ~~~~~
0 errors, 1 warnings""".trimIndent()
)


val incidentRGB = XmlTestSample(
    xmlFilePath = LAYOUT_FILE_PATH,
    xmlSource = """
            <?xml version="1.0" encoding="utf-8"?>
                <androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
                xmlns:app="http://schemas.android.com/apk/res-auto"
                android:layout_width="match_parent"
                android:layout_height="match_parent">
                
                <View
                android:id="@+id/case1"
                android:layout_width="80dp"
                android:layout_height="80dp"
                android:layout_marginTop="32dp"
                android:background="#123"
                app:layout_constraintEnd_toEndOf="parent"
                app:layout_constraintStart_toStartOf="parent"
                app:layout_constraintTop_toTopOf="parent" />
                
            </androidx.constraintlayout.widget.ConstraintLayout>
                """.trimIndent(),
    expectedResult = """res/layout/test_layout.xml:12: Warning: Should use colors only from palette [WrongColorUsage]
    android:background="#123"
                        ~~~~
0 errors, 1 warnings""".trimIndent()
)


val noIncident = XmlTestSample(
    xmlFilePath = LAYOUT_FILE_PATH,
    xmlSource = """
            <?xml version="1.0" encoding="utf-8"?>
                <androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
                xmlns:app="http://schemas.android.com/apk/res-auto"
                android:layout_width="match_parent"
                android:layout_height="match_parent">
                
                    <View
                    android:id="@+id/case4"
                    android:layout_width="80dp"
                    android:layout_height="80dp"
                    android:layout_marginTop="16dp"
                    android:background="@color/teal_200"
                    app:layout_constraintEnd_toEndOf="@+id/case3"
                    app:layout_constraintStart_toStartOf="@+id/case3"
                    app:layout_constraintTop_toBottomOf="@+id/case3" />
                
            </androidx.constraintlayout.widget.ConstraintLayout>
                """.trimIndent(),
    expectedResult = "No warnings."
)

val incidentNonPaletteColorReference = XmlTestSample(
    xmlFilePath = LAYOUT_FILE_PATH,
    xmlSource = """
            <?xml version="1.0" encoding="utf-8"?>
                <androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
                xmlns:app="http://schemas.android.com/apk/res-auto"
                android:layout_width="match_parent"
                android:layout_height="match_parent">
                
                    <View
                    android:id="@+id/case4"
                    android:layout_width="80dp"
                    android:layout_height="80dp"
                    android:layout_marginTop="16dp"
                    android:background="@android:color/holo_blue_dark"
                    app:layout_constraintEnd_toEndOf="@+id/case3"
                    app:layout_constraintStart_toStartOf="@+id/case3"
                    app:layout_constraintTop_toBottomOf="@+id/case3" />
                
            </androidx.constraintlayout.widget.ConstraintLayout>
                """.trimIndent(),
    expectedResult = """res/layout/test_layout.xml:12: Warning: Should use colors only from palette [WrongColorUsage]
        android:background="@android:color/holo_blue_dark"
                            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warnings""".trimIndent()
)

val incidentSelectorBadTwoColors = XmlTestSample(
    xmlFilePath = SELECTOR_FILE_PATH,
    xmlSource = """
            <?xml version="1.0" encoding="utf-8"?>
            <selector xmlns:android="http://schemas.android.com/apk/res/android">
                <item android:color="#5C6BC0" android:state_enabled="true" />
                <item android:color="#C5CAE9" />
            </selector>
                """.trimIndent(),
    expectedResult = """res/layout/test_selector.xml:3: Warning: Should use colors only from palette [WrongColorUsage]
    <item android:color="#5C6BC0" android:state_enabled="true" />
                         ~~~~~~~
res/layout/test_selector.xml:4: Warning: Should use colors only from palette [WrongColorUsage]
    <item android:color="#C5CAE9" />
                         ~~~~~~~
0 errors, 2 warnings""".trimIndent()
)

val incidentVector = XmlTestSample(
    xmlFilePath = VECTOR_FILE_PATH,
    xmlSource = """
                <vector xmlns:android="http://schemas.android.com/apk/res/android"
                    android:width="24dp"
                    android:height="24dp"
                    android:tint="#b4ffff"
                    android:tintMode="multiply"
                    android:viewportWidth="24"
                    android:viewportHeight="24">
                    <path
                        android:fillColor="@color/teal_200"
                        android:pathData="M5,16c0,3.87 3.13,7 7,7s7,-3.13 7,-7v-4L5,12v4zM16.12,4.37l2.1,-2.1 -0.82,-0.83 -2.3,2.31C14.16,3.28 13.12,3 12,3s-2.16,0.28 -3.09,0.75L6.6,1.44l-0.82,0.83 2.1,2.1C6.14,5.64 5,7.68 5,10v1h14v-1c0,-2.32 -1.14,-4.36 -2.88,-5.63zM9,9c-0.55,0 -1,-0.45 -1,-1s0.45,-1 1,-1 1,0.45 1,1 -0.45,1 -1,1zM15,9c-0.55,0 -1,-0.45 -1,-1s0.45,-1 1,-1 1,0.45 1,1 -0.45,1 -1,1z" />
                </vector>
                """.trimIndent(),
    expectedResult = """res/layout/ic_baseline_adb_24.xml:4: Warning: Should use colors only from palette [WrongColorUsage]
    android:tint="#b4ffff"
                  ~~~~~~~
0 errors, 1 warnings""".trimIndent()
)