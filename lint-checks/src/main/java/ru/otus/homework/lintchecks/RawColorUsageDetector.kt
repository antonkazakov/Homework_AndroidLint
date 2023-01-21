package ru.otus.homework.lintchecks

import com.android.tools.lint.detector.api.*
import org.jetbrains.uast.*
import org.w3c.dom.Attr
import org.w3c.dom.Element
import java.util.*

@Suppress("UnstableApiUsage")
class RawColorUsageDetector : ResourceXmlDetector() {

    private val rawColorsInResources = mutableListOf<ColorLocation>()
    private val colorsInPalette = mutableMapOf<String, String>()

    override fun getApplicableAttributes(): Collection<String> = listOf(
        "background",
        "backgroundTint",
        "textColor",
        "color",
        "fillColor",
        "iconTint",
        "tint"
    )

    override fun getApplicableElements(): Collection<String> = listOf("color")

    private fun String.isRawColor(): Boolean = this.matches(
        "^#([a-fA-F0-9]{3}|[a-fA-F0-9]{6}|[a-fA-F0-9]{8})$".toRegex())

    override fun visitAttribute(context: XmlContext, attribute: Attr) {
        val attrValue = attribute.value.lowercase()
        if (attrValue.isRawColor()) {
            rawColorsInResources.add(ColorLocation(
                location = context.getValueLocation(attribute),
                color = attrValue
            ))
        }
    }

    override fun visitElement(context: XmlContext, element: Element) {
        if (context.file.name == PALETTE_FILE) {
            val colorName = element.attributes.item(0).nodeValue.lowercase()
            val colorCode = element.firstChild.nodeValue.lowercase()
            colorsInPalette[colorCode] = colorName
        }
    }

    override fun afterCheckRootProject(context: Context) {
        rawColorsInResources.forEach { rawColor ->
            val colorNameToReplace = colorsInPalette[rawColor.color]
            val location = rawColor.location
            val fix = colorNameToReplace?.let {
                colorFix(location = location, newColor = it)
            }
            context.report(
                issue = ISSUE,
                location = location,
                message = BRIEF_DESCRIPTION,
                quickfixData = fix
            )
        }
    }

    private fun colorFix(location: Location, newColor: String): LintFix =
        fix()
            .replace()
            .range(range = location)
            .all()
            .with(COLOR_PREFIX + newColor)
            .build()

    companion object {

        private const val ID = "RawColorUsage"
        private const val BRIEF_DESCRIPTION =
            "Raw color use is not recommended."
        private const val EXPLANATION =
            "Use themes and pallets to define colors. It will make it possible to reuse colors and your design will be more consistent."
        private const val PRIORITY = 1
        private const val PALETTE_FILE = "colors.xml"
        private const val COLOR_PREFIX = "@color/"

        val ISSUE = Issue.create(
            id = ID,
            briefDescription = BRIEF_DESCRIPTION,
            explanation = EXPLANATION,
            category = Category.LINT,
            priority = PRIORITY,
            severity = Severity.WARNING,
            implementation = Implementation(RawColorUsageDetector::class.java,
                Scope.RESOURCE_FILE_SCOPE)
        )
    }
}

@Suppress("UnstableApiUsage")
data class ColorLocation(
    val location: Location,
    val color: String,
)