package ru.otus.homework.lintchecks.detectors

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
import org.w3c.dom.Attr
import org.w3c.dom.Element
import ru.otus.homework.lintchecks.LintConstants.ISSUE_CATEGORY_NAME
import ru.otus.homework.lintchecks.LintConstants.ISSUE_MAX_PRIORITY
import ru.otus.homework.lintchecks.LintConstants.ISSUE_MEDIUM_PRIORITY

@Suppress("UnstableApiUsage")
class RawColorUsageDetector : ResourceXmlDetector() {

    private val dsColors = mutableMapOf<String, String>()
    private val foundColors = mutableListOf<Color>()

    override fun getApplicableElements(): Collection<String> {
        return listOf("color")
    }

    override fun visitElement(context: XmlContext, element: Element) {
        val fileName = context.getLocation(element).file.name
        if (fileName != "colors.xml") return
        val colorName = element.getAttribute("name")
        val colorValue = element.firstChild.nodeValue
        dsColors[colorName] = colorValue
    }

    override fun getApplicableAttributes(): Collection<String> {
        return listOf("background", "backgroundTint", "color", "tint")
    }

    override fun visitAttribute(context: XmlContext, attribute: Attr) {

        if (!attribute.value.startsWith("#")) return

        foundColors += Color(
            color = attribute.value,
            location = context.getLocation(attribute)
        )
    }

    override fun afterCheckRootProject(context: Context) {
        foundColors.forEach { color:Color ->
            val dsColor = dsColors.filterValues { it == color.color }.map { it.key }.firstOrNull()

            val fix = if (dsColor != null) createFix(dsColor, color.color) else null
            val description =
                if (dsColor != null) "Используйте цвет @color/${dsColor}" else BRIEF_DESCRIPTION

            context.report(
                ISSUE,
                color.location,
                description,
                quickfixData = fix
            )
        }
    }

    private fun createFix(dsColor: String, color: String): LintFix {
        return LintFix.create()
            .replace()
            .text(color)
            .with("@color/${dsColor}")
            .build()
    }

    companion object {
        private const val ID = "RawColorUsage"
        private const val BRIEF_DESCRIPTION =
            "Цвет не из дизайн системы"
        private const val EXPLANATION =
            "Используйте цвета описанные в colors.xml"

        val ISSUE = Issue.create(
            id = ID,
            briefDescription = BRIEF_DESCRIPTION,
            explanation = EXPLANATION,
            category = Category.create(ISSUE_CATEGORY_NAME, ISSUE_MAX_PRIORITY),
            priority = ISSUE_MEDIUM_PRIORITY,
            severity = Severity.WARNING,
            implementation = Implementation(
                RawColorUsageDetector::class.java,
                Scope.RESOURCE_FILE_SCOPE
            )
        )
    }

    data class Color(
        val color: String,
        val location: Location
    )

}