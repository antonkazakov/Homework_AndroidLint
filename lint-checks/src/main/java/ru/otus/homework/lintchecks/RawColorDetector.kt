package ru.otus.homework.lintchecks

import com.android.resources.ResourceFolderType
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
private const val BRIEF_DESCRIPTION =
    "Используйте цвет из палитры colors.xml"
private const val EXPLANATION =
    "Используйте цвет из палитры colors.xml. " +
            "Использование цветов не из палитры может привести к неконсистентному дизайну"

private const val PRIORITY = 6

private const val TAG_COLOR = "color"

class RawColorDetector: ResourceXmlDetector() {
    companion object {

        val ISSUE = Issue.create(
            ID,
            BRIEF_DESCRIPTION,
            EXPLANATION,
            Category.create("TEST CATEGORY", 10),
            PRIORITY,
            Severity.WARNING,
            Implementation(RawColorDetector::class.java, Scope.RESOURCE_FILE_SCOPE)
        )
    }

    private val colorMap = mutableMapOf<String, MutableList<String>>()
    private val rawColors = ArrayList<Pair<Location, String>>()

    override fun appliesTo(folderType: ResourceFolderType): Boolean {
        return folderType == ResourceFolderType.DRAWABLE ||
                folderType == ResourceFolderType.LAYOUT ||
                folderType == ResourceFolderType.VALUES ||
                folderType == ResourceFolderType.COLOR
    }

    override fun visitAttribute(context: XmlContext, attribute: Attr) {
        val value = attribute.value

        if (value.isNullOrBlank() || !value.isColor()) return

        val location = context.getValueLocation(attribute)

        rawColors.add(
            Pair(
                first = location,
                second = value
            )
        )
    }

    override fun visitElement(context: XmlContext, element: Element) {
        if (element.tagName != TAG_COLOR) return

        val color = element.firstChild.nodeValue.lowercase()

        if (!color.isColor()) return

        val name = element.attributes.item(0)?.nodeValue?.lowercase() ?: return

        if (!colorMap.containsKey(color)) {
            colorMap[color] = mutableListOf(name)
        } else {
            colorMap[color]?.add(name)
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
                createFix(replacements, rawColor.first)
            )
        }
    }

    private fun createFix(
        replacements: List<String>?,
        location: Location
    ): LintFix? {
        return replacements?.let {
            fix().alternatives(*it.map { value ->
                fix()
                    .replace()
                    .range(location)
                    .all()
                    .with("@color/$value")
                    .build()
            }.toTypedArray())
        }
    }

    override fun getApplicableAttributes(): Collection<String> = XmlScannerConstants.ALL

    override fun getApplicableElements(): Collection<String> = XmlScannerConstants.ALL

    private fun String.isColor(): Boolean = startsWith("#") && (length == 7 || length == 9)
}