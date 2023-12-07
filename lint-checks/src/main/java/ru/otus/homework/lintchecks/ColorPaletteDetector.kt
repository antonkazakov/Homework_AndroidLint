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

private const val ID = "RawColorUsage"
private const val BRIEF_DESCRIPTION = "Используйте цвета из палитры colors.xml"
private const val EXPLANATION = """Чтобы получить максимальную пользу от дизайн системы, использование цветов должно быть ограничено установленной палитрой."""

class ColorPaletteDetector : ResourceXmlDetector() {
    companion object {
        val ISSUE = Issue.create(
            ID,
            BRIEF_DESCRIPTION,
            EXPLANATION,
            Category.create("TEST CATEGORY", 10),
            6,
            Severity.WARNING,
            Implementation(
                ColorPaletteDetector::class.java,
                Scope.RESOURCE_FILE_SCOPE
            )
        )
    }

    private val colorMap = mutableMapOf<String, MutableList<String>>()
    private val rawColors = ArrayList<Pair<Location, String>>()

    override fun getApplicableAttributes(): Collection<String> = XmlScannerConstants.ALL

    override fun getApplicableElements(): Collection<String> = XmlScannerConstants.ALL

    override fun visitAttribute(context: XmlContext, attribute: Attr) {
        val value = attribute.value
        if (value?.isColor() == true) {
            rawColors.add(Pair(context.getValueLocation(attribute), value))
        }
    }

    override fun visitElement(context: XmlContext, element: Element) {
        if (element.tagName == "color") {
            val color = element.firstChild.nodeValue.lowercase()
            if (color.isColor()) {
                val name = element.attributes.item(0)?.nodeValue?.lowercase() ?: return
                if (!colorMap.containsKey(color)) {
                    colorMap[color] = mutableListOf(name)
                } else {
                    colorMap[color]?.add(name)
                }
            }
        }
    }

    override fun beforeCheckRootProject(context: Context) {
        colorMap.clear()
        rawColors.clear()
    }

    override fun afterCheckRootProject(context: Context) {
        rawColors.forEach { rawColor ->
            val replacements = colorMap[rawColor.second.lowercase()]
            context.report(
                ISSUE,
                rawColor.first,
                BRIEF_DESCRIPTION,
                quickFix(replacements, rawColor.first)
            )
        }
    }

    private fun quickFix(replacementValues: List<String>?, location: Location): LintFix? {
        return replacementValues?.run {
            fix().alternatives(*mapNotNull { replacementValue ->
                fix()
                    .replace()
                    .range(location)
                    .all()
                    .with("@color/$replacementValue")
                    .build()
            }.toTypedArray())
        }
    }

    private fun String.isColor(): Boolean = startsWith("#") && (length == 7 || length == 9)
}