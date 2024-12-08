package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.xml
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

@Suppress("UnstableApiUsage")
internal class WrongColorUsageDetectorTest {
    private val lintTask = lint().allowMissingSdk().issues(WrongColorUsageDetector.ISSUE)

    @Test
    fun `should detect AARRGGBB`() {
        lintTask.files(
            xml(incidentAARRGGBB.xmlFilePath,
                incidentAARRGGBB.xmlSource))
            .run()
            .expect(incidentAARRGGBB.expectedResult)
    }

    @Test
    fun `should detect RRGGBB`() {
        lintTask.files(
            xml(incidentRRGGBB.xmlFilePath,
                incidentRRGGBB.xmlSource))
            .run()
            .expect(incidentRRGGBB.expectedResult)
    }

    @Test
    fun `should detect ARGB`() {
        lintTask.files(
            xml(incidentARGB.xmlFilePath,
                incidentARGB.xmlSource))
            .run()
            .expect(incidentARGB.expectedResult)
    }

    @Test
    fun `should detect RGB`() {
        lintTask.files(
            xml(incidentRGB.xmlFilePath,
                incidentRGB.xmlSource))
            .run()
            .expect(incidentRGB.expectedResult)
    }


    @Test
    fun `should detect nothing`() {
        lintTask.files(
            xml(noIncident.xmlFilePath,
                noIncident.xmlSource))
            .run()
            .expect(noIncident.expectedResult)
    }


    // здесь непонятный случай - здесь есть android:background="@android:color/holo_blue_dark", цвет есть
    // в палитре, но ссылка идет не на палитру. Наверное, надо тожет рапортовать и предлагать замену
    @Test
    fun `should detect and suggest replacement`() {
        lintTask.files(
            xml(incidentNonPaletteColorReference.xmlFilePath,
                incidentNonPaletteColorReference.xmlSource))
            .run()
            .expect(incidentNonPaletteColorReference.expectedResult)
    }

    @Test
    fun `should detect two issues in selector`() {
        lintTask.files(
            xml(incidentSelectorBadTwoColors.xmlFilePath,
                incidentSelectorBadTwoColors.xmlSource))
            .run()
            .expect(incidentSelectorBadTwoColors.expectedResult)
    }

    @Test
    fun `should detect one issues in vector`() {
        lintTask.files(
            xml(incidentVector.xmlFilePath,
                incidentVector.xmlSource))
            .run()
            .expect(incidentVector.expectedResult)
    }
}