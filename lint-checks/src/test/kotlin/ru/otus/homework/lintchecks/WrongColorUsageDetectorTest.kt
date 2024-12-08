package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.xml
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

@Suppress("UnstableApiUsage")
internal class WrongColorUsageDetectorTest {
    private val lintTask = lint().allowMissingSdk().issues(WrongColorUsageDetector.ISSUE)

    @Test
    fun `should detect AARRGGBB`() {
        runLintTask(
            incidentAARRGGBB.xmlFilePath,
            incidentAARRGGBB.xmlSource,
            incidentAARRGGBB.expectedResult
        )
    }

    @Test
    fun `should detect RRGGBB`() {
        runLintTask(
            incidentRRGGBB.xmlFilePath,
            incidentRRGGBB.xmlSource,
            incidentRRGGBB.expectedResult
        )
    }

    @Test
    fun `should detect ARGB`() {
        runLintTask(
            incidentARGB.xmlFilePath,
            incidentARGB.xmlSource,
            incidentARGB.expectedResult
        )
    }

    @Test
    fun `should detect RGB`() {
        lintTask.files(
            xml(
                incidentRGB.xmlFilePath,
                incidentRGB.xmlSource
            )
        )
            .run()
            .expect(incidentRGB.expectedResult)
    }


    @Test
    fun `should detect nothing`() {
        runLintTask(
            noIncident.xmlFilePath,
            noIncident.xmlSource,
            noIncident.expectedResult
        )
    }


    // здесь непонятный случай - здесь есть android:background="@android:color/holo_blue_dark", цвет есть
    // в палитре, но ссылка идет не на палитру. Наверное, надо тожет рапортовать и предлагать замену
    @Test
    fun `should detect and suggest replacement`() {
        runLintTask(
            incidentNonPaletteColorReference.xmlFilePath,
            incidentNonPaletteColorReference.xmlSource,
            incidentNonPaletteColorReference.expectedResult
        )
    }

    @Test
    fun `should detect two issues in selector`() {
        runLintTask(
            incidentSelectorBadTwoColors.xmlFilePath,
            incidentSelectorBadTwoColors.xmlSource,
            incidentSelectorBadTwoColors.expectedResult
        )
    }

    @Test
    fun `should detect one issues in vector`() {
        runLintTask(
            incidentVector.xmlFilePath,
            incidentVector.xmlSource,
            incidentVector.expectedResult
        )
    }

    private fun runLintTask(xmlFilePath: String, xmlSource: String, expectedResult: String) {
        lintTask.files(
            mockColorPalette,
            xml(xmlFilePath, xmlSource)
        )
            .run()
            .expect(expectedResult)
    }

    private val mockColorPalette =
        xml("res/values/colors.xml",
            """<?xml version="1.0" encoding="utf-8"?>
            <resources>
                <color name="purple_200">#FFBB86FC</color>
                <color name="purple_500">#FF6200EE</color>
                <color name="purple_700">#FF3700B3</color>
                <color name="teal_200">#FF03DAC5</color>
                <color name="teal_700">#FF018786</color>
                <color name="black">#FF000000</color>
                <color name="white">#FFFFFFFF</color>
            </resources>
            """.trimIndent())
}