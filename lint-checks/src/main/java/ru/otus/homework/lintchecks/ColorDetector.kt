package ru.otus.homework.lintchecks

import org.w3c.dom.Attr
import org.w3c.dom.Element
import com.android.resources.ResourceFolderType
import com.android.tools.lint.detector.api.*

@Suppress("UnstableApiUsage")
class ColorDetector : ResourceXmlDetector() {

    private val projectColors = mutableMapOf<String, ArrayList<String>>()
    private val rawColors = mutableListOf<RawColor>()
    private val resourceTypes = setOf(
        ResourceFolderType.DRAWABLE,
        ResourceFolderType.LAYOUT,
        ResourceFolderType.VALUES,
        ResourceFolderType.COLOR,
    )
    private val colorRegex = "^#([a-fA-F0-9]{6}|[a-fA-F0-9]{8})\$".toRegex()

    override fun appliesTo(folderType: ResourceFolderType) = resourceTypes.contains(folderType)

    override fun visitAttribute(context: XmlContext, attribute: Attr) {
        if (!attribute.isColor()) return

        rawColors.add(
            RawColor(
                location = context.getValueLocation(attribute),
                color = attribute.value.lowercase()
            )
        )
    }

    override fun visitElement(context: XmlContext, element: Element) {
        if (!context.file.path.contains(COLORS_FILE)) return
        val color = element.firstChild.nodeValue.lowercase()
        if (!color.isColor()) return

        val name = element.attributes.item(0)?.nodeValue?.lowercase() ?: return
        if (!projectColors.containsKey(color)) projectColors[color] = ArrayList()
        projectColors[color]?.add(name)
    }

    override fun beforeCheckRootProject(context: Context) {
        projectColors.clear()
        rawColors.clear()
    }

    override fun afterCheckRootProject(context: Context) {
        rawColors.forEach { rawColor ->
            val colorName = projectColors[rawColor.color]
            context.report(
                ISSUE,
                rawColor.location,
                BRIEF_DESCRIPTION,
                quickfixData = if (colorName != null) createFix(colorName, rawColor.location) else null
            )
        }
    }

    private fun createFix(
        colorNames: List<String>,
        location: Location
    ): LintFix = fix().alternatives(*colorNames.map { value ->
        fix()
            .replace()
            .range(location)
            .all()
            .with("@color/$value")
            .build()
    }.toTypedArray())

    override fun getApplicableAttributes(): Collection<String> = XmlScannerConstants.ALL

    override fun getApplicableElements(): Collection<String> = XmlScannerConstants.ALL

    private fun Attr.isColor() = this.value != null && this.value.isColor()

    private fun String.isColor() = matches(colorRegex)

    private class RawColor(
        val location: Location,
        val color: String,
    )

    companion object {
        private const val ID = "ColorsUsage"
        private const val BRIEF_DESCRIPTION = "Использование цветов не из палитры недопустимо"
        private const val EXPLANATION = "Замените на название цвета из colors.xml"
        private const val COLORS_FILE = "colors.xml"

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
}