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
import java.util.regex.Pattern

@Suppress("UnstableApiUsage")
internal class WrongColorUsageDetector : ResourceXmlDetector() {

    // matches hexadecimal colors of lengths 3, 4, 6, or 8
    private val COLOR_PATTERN = Pattern.compile("^#(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$")

    private data class ColorToLocation(
        val location: Location,
        val color: String
    )

    private val hexColors = mutableListOf<ColorToLocation>()
    private val paletteColors = mutableMapOf<String, String>()

    override fun getApplicableAttributes(): Collection<String>? {
        return XmlScannerConstants.ALL
    }

    override fun getApplicableElements(): Collection<String> {
        return XmlScannerConstants.ALL
    }

    override fun visitAttribute(context: XmlContext, attribute: Attr) {
        val attrValue = attribute.value.orEmpty()
        if (isHexColor(attrValue)) {
            normalizeColor(attrValue)?.let {
                hexColors.add(
                    ColorToLocation(
                        location = context.getValueLocation(attribute),
                        color = it
                    )
                )
            }
            println("hexColors: ${hexColors.map { it.color }}")
        }
    }

    private fun isHexColor(value: String): Boolean {
        return COLOR_PATTERN.matcher(value).matches()
    }

    override fun visitElement(context: XmlContext, element: Element) {
        if (context.file.name == PALETTE_FILE) {
            val colorName = element.attributes.item(0)?.nodeValue?.lowercase() ?: return
            val colorCode = element.firstChild.nodeValue.lowercase()
            paletteColors[colorCode] = colorName
        }
    }

    override fun afterCheckRootProject(context: Context) {
        hexColors.forEach { rawColor ->
            val colorNameToReplace = paletteColors[rawColor.color]
            val location = rawColor.location
            val fix = colorNameToReplace?.let {
                quickColorFix(location = location, newColor = it)
            }
            context.report(
                issue = ISSUE,
                location = location,
                message = BRIEF_DESCRIPTION,
                quickfixData = fix
            )
        }
    }

    private fun quickColorFix(location: Location, newColor: String): LintFix {
        return fix()
            .replace()
            .range(location)
            .all()
            .with(COLOR_PREFIX + newColor)
            .build()
    }

    companion object {
        private const val ID = "WrongColorUsage"
        private const val BRIEF_DESCRIPTION = "Should use colors only from palette"
        private const val EXPLANATION =
            "All app colors should be taken from the color palette defined in `colors.xml`"
        private const val PRIORITY = 6
        private const val PALETTE_FILE = "colors.xml"
        private const val COLOR_PREFIX = "@color/"

        val ISSUE = Issue.create(
            id = ID,
            briefDescription = BRIEF_DESCRIPTION,
            explanation = EXPLANATION,
            category = Category.CORRECTNESS,
            priority = PRIORITY,
            severity = Severity.WARNING,
            implementation = Implementation(
                WrongColorUsageDetector::class.java,
                Scope.RESOURCE_FILE_SCOPE
            )
        )
    }
}