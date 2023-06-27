package ru.otus.homework.lintchecks

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.ResourceXmlDetector
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.XmlContext
import com.android.tools.lint.detector.api.XmlScannerConstants
import org.w3c.dom.Attr
import org.w3c.dom.Element

@Suppress("UnstableApiUsage")
class RawColorUsageDetector : ResourceXmlDetector() {

    private val rawColors = mutableListOf<RawColor>()
    private val paletteColors = mutableMapOf<String, String>()

    override fun getApplicableAttributes(): Collection<String>? {
        return XmlScannerConstants.ALL
    }

    override fun getApplicableElements(): Collection<String> {
        return XmlScannerConstants.ALL
    }

    override fun visitAttribute(context: XmlContext, attribute: Attr) {
        val attrValue = attribute.value.orEmpty()
        if (attrValue.isRawColor()) {
            rawColors.add(
                RawColor(
                    location = context.getValueLocation(attribute),
                    color = attrValue.lowercase()
                )
            )
        }
    }

    override fun visitElement(context: XmlContext, element: Element) {
        if (context.file.name == PALETTE_FILE) {
            val colorName = element.attributes.item(0)?.nodeValue?.lowercase() ?: return
            val colorCode = element.firstChild.nodeValue.lowercase()
            paletteColors[colorCode] = colorName
        }
    }

    override fun afterCheckRootProject(context: Context) {
        rawColors.forEach { rawColor ->
            val colorNameToReplace = paletteColors[rawColor.color]
            val location = rawColor.location
            val fix = colorNameToReplace?.let {
                createColorFix(location = location, newColor = it)
            }
            context.report(
                issue = ISSUE,
                location = location,
                message = BRIEF_DESCRIPTION,
                quickfixData = fix
            )
        }
    }

    private fun createColorFix(location: Location, newColor: String): LintFix {
        return fix()
            .replace()
            .range(location)
            .all()
            .with(COLOR_PREFIX + newColor)
            .build()
    }

    private fun String.isRawColor(): Boolean {
        return this.matches("^#([a-fA-F0-9]{3}|[a-fA-F0-9]{6}|[a-fA-F0-9]{8})$".toRegex())
    }

    data class RawColor(
        val location: Location,
        val color: String
    )

    companion object {
        private const val ID = "RawColorUsage"
        private const val BRIEF_DESCRIPTION = "Используемые цвета должны браться из палитры."
        private const val EXPLANATION =
            "Все цвета, которые используются в ресурсах приложения должны находится в палитре. За палитру следует принимать цвета, описанные в файле `colors.xml`"
        private const val PRIORITY = 6
        private const val PALETTE_FILE = "colors.xml"
        private const val COLOR_PREFIX = "@color/"

        val ISSUE = Issue.create(
            id = ID,
            briefDescription = BRIEF_DESCRIPTION,
            explanation = EXPLANATION,
            category = Category.LINT,
            priority = PRIORITY,
            severity = Severity.WARNING,
            implementation = Implementation(
                RawColorUsageDetector::class.java,
                Scope.RESOURCE_FILE_SCOPE
            )
        )
    }
}
