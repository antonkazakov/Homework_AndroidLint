package ru.otus.homework.lintchecks


import com.android.resources.ResourceFolderType
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.ResourceXmlDetector
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.XmlContext
import org.w3c.dom.Attr
import org.w3c.dom.Element

@Suppress("UnstableApiUsage")
class RawColorRule : ResourceXmlDetector(), Detector.XmlScanner {

    private val projectColorMap = mutableMapOf<String, String>()
    private val rawColorList = mutableListOf<Pair<Location, String>>()

    override fun appliesTo(folderType: ResourceFolderType): Boolean {
        return listOf(
            ResourceFolderType.DRAWABLE,
            ResourceFolderType.LAYOUT,
            ResourceFolderType.VALUES,
            ResourceFolderType.COLOR
        ).contains(folderType)
    }

    override fun getApplicableAttributes(): Collection<String> {
        return ALL
    }

    override fun getApplicableElements(): Collection<String> {
        return ALL
    }

    override fun beforeCheckRootProject(context: Context) {
        rawColorList.clear()
        projectColorMap.clear()
    }

    override fun visitAttribute(context: XmlContext, attribute: Attr) {
        val attributeValue = attribute.value ?: return

        if (!attributeValue.isRawColor()) return

        rawColorList.add(
            context.getValueLocation(attribute) to attributeValue.lowercase()
        )
    }

    override fun visitElement(context: XmlContext, element: Element) {
        if (!context.file.path.contains("colors.xml")) return

        val colorValue = element.firstChild.nodeValue.lowercase()

        if (!colorValue.isRawColor()) return

        val colorName = element.attributes.item(0)?.nodeValue?.lowercase() ?: return

        projectColorMap[colorValue] = colorName
    }

    override fun afterCheckRootProject(context: Context) {
        rawColorList.forEach { (location, color) ->
            context.report(
                issue = ISSUE,
                location = location,
                message = DESCRIPTION,
                quickfixData = getQuickFix(location, color)
            )
        }
    }

    private fun getQuickFix(
        location: Location,
        color: String
    ): LintFix? {
        val colorName = projectColorMap[color] ?: return null

        return fix().replace()
            .range(location).all()
            .with("$@color/$colorName")
            .build()
    }

    private fun String.isRawColor(): Boolean {
        return matches("^#([a-fA-F0-9]{3,4}|[a-fA-F0-9]{6}|[a-fA-F0-9]{8})\$".toRegex())
    }


    companion object {

        private const val DESCRIPTION = "RawColor usage is now allowed"

        val ISSUE = Issue.create(
            id = "RawColor",
            briefDescription = DESCRIPTION,
            explanation = "All colors must be declared in resources xml file",
            category = Category.CUSTOM_LINT_CHECKS,
            priority = 5,
            severity = Severity.ERROR,
            implementation = Implementation(
                RawColorRule::class.java,
                Scope.RESOURCE_FILE_SCOPE
            )
        )
    }
}
