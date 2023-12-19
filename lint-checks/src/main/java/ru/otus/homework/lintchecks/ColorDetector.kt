package ru.otus.homework.lintchecks

import org.w3c.dom.Element
import org.w3c.dom.Attr
import com.android.resources.ResourceFolderType
import com.android.tools.lint.detector.api.*

private const val ID = "ArbitraryColorsUsage"
private const val BRIEF_DESCRIPTION = "Usage the color arbitrary isn`t allowed"
private const val EXPLANATION = "Need to use colors resource value"

@Suppress("UnstableApiUsage")
class ColorDetector : ResourceXmlDetector() {

    private val arbitraryColors = ArrayList<Pair<String, Location>>()
    private val colorMap = HashMap<String, ArrayList<String>>()

    companion object {
        val ISSUE = Issue.create(
            id = ID,
            briefDescription = BRIEF_DESCRIPTION,
            explanation = EXPLANATION,
            category = Category.LINT,
            priority = 6,
            severity = Severity.WARNING,
            implementation = Implementation(
                ColorDetector::class.java,
                Scope.RESOURCE_FILE_SCOPE
            ),
            suppressAnnotations = listOf(ID),
        )
    }

    override fun appliesTo(folderType: ResourceFolderType) =
        folderType == ResourceFolderType.LAYOUT
                || folderType == ResourceFolderType.DRAWABLE
                || folderType == ResourceFolderType.VALUES

    override fun getApplicableAttributes(): Collection<String> = XmlScannerConstants.ALL
    override fun getApplicableElements(): Collection<String> = XmlScannerConstants.ALL

    override fun visitAttribute(context: XmlContext, attribute: Attr) {
        val value = attribute.value ?: return
        if (value.isColor()) {
            arbitraryColors.add(value.lowercase() to context.getValueLocation(attribute))
        }
    }

    override fun visitElement(context: XmlContext, element: Element) {
        if (!context.file.path.contains("colors.xml")) return
        val value = element.firstChild.nodeValue.lowercase()
        if (value.isColor()) {
            val colorName = element.attributes.item(0)?.nodeValue ?: return
            if (!colorMap.containsKey(value))
                colorMap[value] = ArrayList()
            colorMap[value]!!.add(colorName)
        }
    }

    override fun beforeCheckRootProject(context: Context) {
        arbitraryColors.clear()
        colorMap.clear()
    }

    override fun afterCheckRootProject(context: Context) {
        arbitraryColors.forEach {
            val fixValues = colorMap[it.first]
            context.report(
                ISSUE,
                it.second,
                BRIEF_DESCRIPTION,
                quickfixData = createFix(fixValues, it.second)
            )
        }
    }

    private fun createFix(
        fixValues: List<String>?,
        location: Location
    ) = fixValues?.let {
        fix().alternatives(*it.map {
            fix()
                .replace()
                .range(location)
                .all()
                .with("@color/$it")
                .build()
        }.toTypedArray())
    }

    private fun String.isColor(): Boolean = (length == 7 || length == 9) && startsWith("#")
}