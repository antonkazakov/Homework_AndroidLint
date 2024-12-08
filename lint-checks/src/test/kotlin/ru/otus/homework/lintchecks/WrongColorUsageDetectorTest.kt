package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.xml
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

@Suppress("UnstableApiUsage")
internal class WrongColorUsageDetectorTest {
    private val lintTask = lint().allowMissingSdk().issues(WrongColorUsageDetector.ISSUE)

    @Test
    fun `should suggest replace black color`() {
        lintTask.files(
            xml(shouldDetectBlackColorAndSuggestReplacement.xmlFilePath,
                shouldDetectBlackColorAndSuggestReplacement.xmlSource))
            .run()
            .expect(shouldDetectBlackColorAndSuggestReplacement.expectedResult)
    }

    @Test
    fun `should detect nothing`() {
        lintTask.files(
            xml(shouldDetectNothing.xmlFilePath,
                shouldDetectNothing.xmlSource))
            .run()
            .expect(shouldDetectNothing.expectedResult)
    }


    // здесь непонятный случай - здесь есть android:background="@android:color/holo_blue_dark", цвет есть
    // в палитре, но ссылка идет не на палитру. Наверное, надо тожет рапортовать и предлагать замену
    @Test
    fun `should detect and suggest replacement`() {
        lintTask.files(
            xml(shouldDetectAndSuggestReplacement.xmlFilePath,
                shouldDetectAndSuggestReplacement.xmlSource))
            .run()
            .expect(shouldDetectAndSuggestReplacement.expectedResult)
    }

    @Test
    fun `should detect two issues in selector`() {
        lintTask.files(
            xml(shouldDetectTwoColors.xmlFilePath,
                shouldDetectTwoColors.xmlSource))
            .run()
            .expect(shouldDetectTwoColors.expectedResult)
    }

    @Test
    fun `should detect one issues in vector`() {
        lintTask.files(
            xml(shouldDetectOneInVector.xmlFilePath,
                shouldDetectOneInVector.xmlSource))
            .run()
            .expect(shouldDetectOneInVector.expectedResult)
    }
}