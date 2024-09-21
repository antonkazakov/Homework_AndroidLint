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
class ColorUsageDetector : ResourceXmlDetector(), Detector.XmlScanner {
    companion object {
        private const val ISSUE_ID = "ColorNotFromDesignUsage"
        private const val BRIEF_DESCRIPTION = "Color not from design system"
        private const val EXPLANATION =
            "Colors should be defined in the design system and reused throughout the app for consistency."
        val ISSUE = Issue.create(
            id = ISSUE_ID,
            briefDescription = BRIEF_DESCRIPTION,
            explanation = EXPLANATION,
            category = Category.CORRECTNESS,
            priority = 6,
            severity = Severity.ERROR,
            implementation = Implementation(
                ColorUsageDetector::class.java,
                Scope.RESOURCE_FILE_SCOPE
            )
        )
    }

    private val colorNameMap = mutableMapOf<String, String>()
    private val invalidColorLocations = mutableListOf<Pair<Location, String>>()

    override fun appliesTo(folderType: ResourceFolderType): Boolean {
        return folderType in setOf(
            ResourceFolderType.COLOR,
            ResourceFolderType.VALUES,
            ResourceFolderType.LAYOUT,
            ResourceFolderType.DRAWABLE,
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
                issue = ISSUE,
                location = location,
                message = BRIEF_DESCRIPTION,
                quickfixData = createFix(location, color)
            )
        }
    }

    private fun createFix(location: Location, color: String): LintFix? {
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
}