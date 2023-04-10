package ru.otus.homework.lintchecks

import org.w3c.dom.Attr
import org.w3c.dom.Element
import com.android.resources.ResourceFolderType
import com.android.tools.lint.detector.api.*

private const val ID = "ArbitraryColorsUsage"
private const val BRIEF_DESCRIPTION = "Usage of arbitrary colors isn`t allowed"
private const val EXPLANATION = "Use pre-defined colors"
private const val COLORS_FILE = "colors.xml"

@Suppress("UnstableApiUsage")
class ColorDetector : ResourceXmlDetector() {

    companion object {
        val ISSUE = Issue.create(
            id = ID,
            briefDescription = BRIEF_DESCRIPTION,
            explanation = EXPLANATION,
            category = Category.LINT,
            priority = 5,
            severity = Severity.ERROR,
            implementation = Implementation(
                ColorDetector::class.java,
                Scope.RESOURCE_FILE_SCOPE
            )
        )
    }

    private val colorMap = HashMap<String, ArrayList<String>>()
    private val arbitraryColors = ArrayList<Pair<Location, String>>()

    override fun appliesTo(folderType: ResourceFolderType): Boolean {
        return folderType == ResourceFolderType.DRAWABLE ||
                folderType == ResourceFolderType.LAYOUT ||
                folderType == ResourceFolderType.VALUES
    }

    override fun visitAttribute(context: XmlContext, attribute: Attr) {
        val value = attribute.value

        if (value.isNullOrBlank() || !value.isColor()) return

        arbitraryColors.add(
            Pair(
                first = context.getValueLocation(attribute),
                second = value.lowercase()
            )
        )
    }

    override fun visitElement(context: XmlContext, element: Element) {
        if (!context.file.path.contains(COLORS_FILE)) return

        val color = element.firstChild.nodeValue.lowercase()

        if (!color.isColor()) return

        val name = element.attributes.item(0)?.nodeValue?.lowercase() ?: return

        if (!colorMap.containsKey(color)) colorMap[color] = ArrayList()

        colorMap[color]?.add(name)
    }

    override fun beforeCheckRootProject(context: Context) {
        colorMap.clear()
        arbitraryColors.clear()
    }

    override fun afterCheckRootProject(context: Context) {
        arbitraryColors.forEach { rawColor ->
            val fixValues = colorMap[rawColor.second]
            context.report(
                ISSUE,
                rawColor.first,
                BRIEF_DESCRIPTION,
                quickfixData = createFix(fixValues, rawColor.first)
            )
        }
    }

    private fun createFix(
        fixValues: List<String>?,
        location: Location
    ): LintFix? {
        return fixValues?.let {
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