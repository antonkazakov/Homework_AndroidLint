package ru.otus.homework.lintchecks.rules

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
class RawColorUsageRule : ResourceXmlDetector(), Detector.XmlScanner {

    companion object {
        val ISSUE = Issue.create(
            id = RULE_ID,
            briefDescription = RULE_DESCRIPTION,
            explanation = RULE_EXPLANATION,
            category = Category.CUSTOM_LINT_CHECKS,
            priority = RULE_PRIORITY,
            severity = Severity.ERROR,
            implementation = Implementation(
                RawColorUsageRule::class.java,
                Scope.RESOURCE_FILE_SCOPE
            )
        )
    }

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
        if (!context.file.path.contains(COLORS_FILE_NAME)) return

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
                message = RULE_DESCRIPTION,
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
            .with("$COLOR_RESOURCE_TAG/$colorName")
            .build()
    }

    private fun String.isRawColor(): Boolean {
        return matches(HEX_COLOR_REGEX_PATTERN.toRegex())
    }
}

private const val RULE_ID = "RawColorUsage"
private const val RULE_DESCRIPTION = "RawColor usage is now allowed"
private const val RULE_EXPLANATION = "All raw color must be declared in colors.xml"
private const val RULE_PRIORITY = 6

private const val COLOR_RESOURCE_TAG = "@color"
private const val COLORS_FILE_NAME = "colors.xml"

private const val HEX_COLOR_REGEX_PATTERN = "^#([a-fA-F0-9]{3,4}|[a-fA-F0-9]{6}|[a-fA-F0-9]{8})\$"
