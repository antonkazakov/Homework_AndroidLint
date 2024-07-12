package ru.otus.homework.lintchecks

import com.android.resources.ResourceFolderType
import com.android.tools.lint.detector.api.*
import org.w3c.dom.Attr
import org.w3c.dom.Element

@Suppress("UnstableApiUsage")
class CustomColorDetector : ResourceXmlDetector(), Detector.XmlScanner {

    private val colorNameMap = mutableMapOf<String, String>()
    private val invalidColorLocations = mutableListOf<Pair<Location, String>>()

    override fun appliesTo(folderType: ResourceFolderType): Boolean {
        return folderType in setOf(
            ResourceFolderType.DRAWABLE,
            ResourceFolderType.LAYOUT,
            ResourceFolderType.VALUES,
            ResourceFolderType.COLOR
        )
    }

    override fun getApplicableAttributes(): Collection<String>? {
        return ALL
    }

    override fun getApplicableElements(): Collection<String>? {
        return ALL
    }

    override fun beforeCheckRootProject(context: Context) {
        invalidColorLocations.clear()
        colorNameMap.clear()
    }

    override fun visitAttribute(context: XmlContext, attribute: Attr) {
        val attributeValue = attribute.value ?: return
        if (!attributeValue.isHexColor()) return

        invalidColorLocations.add(
            context.getValueLocation(attribute) to attributeValue.lowercase()
        )
    }

    override fun visitElement(context: XmlContext, element: Element) {
        if (!context.file.name.contains("colors.xml")) return

        val colorValue = element.firstChild?.nodeValue?.lowercase() ?: return
        if (!colorValue.isHexColor()) return

        val colorName = element.getAttribute("name").lowercase()
        colorNameMap[colorValue] = colorName
    }

    override fun afterCheckRootProject(context: Context) {
        invalidColorLocations.forEach { (location, color) ->
            context.report(
                issue = COLOR_ISSUE,
                location = location,
                message = COLOR_DESCRIPTION,
                quickfixData = createQuickFix(location, color)
            )
        }
    }

    private fun createQuickFix(location: Location, color: String): LintFix? {
        val colorName = colorNameMap[color] ?: return null

        return fix().replace()
            .range(location)
            .all()
            .with("@color/$colorName")
            .build()
    }

    private fun String.isHexColor(): Boolean {
        return matches("^#([a-fA-F0-9]{3,4}|[a-fA-F0-9]{6}|[a-fA-F0-9]{8})$".toRegex())
    }

    companion object {
        private const val COLOR_DESCRIPTION = "Invalid color format detected"
        val COLOR_ISSUE = Issue.create(
            id = "InvalidColorFormat",
            briefDescription = COLOR_DESCRIPTION,
            explanation = "Colors must use the correct hex format in XML files",
            category = Category.CORRECTNESS,
            priority = 5,
            severity = Severity.ERROR,
            implementation = Implementation(
                CustomColorDetector::class.java,
                Scope.RESOURCE_FILE_SCOPE
            )
        )
    }
}
