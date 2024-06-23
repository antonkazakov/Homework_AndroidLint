@file:Suppress("UnstableApiUsage")

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
import org.w3c.dom.Attr
import org.w3c.dom.Element

class ColorUsageDetector : ResourceXmlDetector() {

    private val projectColors = mutableListOf<ColorModel>()
    private val issues = mutableListOf<IssueModel>()

    override fun getApplicableElements(): Collection<String> {
        return listOf("color")
    }

    override fun visitElement(context: XmlContext, element: Element) {
        val fileName = context.getLocation(element).file.name
        if (fileName != "colors.xml") return
        val colorName = element.getAttribute("name")
        val colorValue = element.firstChild.nodeValue
        projectColors += ColorModel(
            name = colorName,
            color = colorValue
        )
    }

    override fun getApplicableAttributes(): Collection<String> {
        return listOf("background", "backgroundTint", "color", "tint")
    }

    override fun visitAttribute(context: XmlContext, attribute: Attr) {

        if (!attribute.value.startsWith("#")) return

        issues += IssueModel(
            color = attribute.value,
            location = context.getLocation(attribute)
        )
    }

    override fun afterCheckRootProject(context: Context) {
        for (issue in issues) {
            val projectColor = projectColors.find { it.color == issue.color }
            val fix = if (projectColor != null) createFix(projectColor, issue.color) else null
            val description =
                if (projectColor != null) "Используйте цвет @color/${projectColor.name}" else DESCRIPTION
            context.report(
                ISSUE,
                issue.location,
                description,
                quickfixData = fix
            )
        }
    }

    private fun createFix(projectColor: ColorModel, color: String): LintFix {
        return LintFix.create()
            .replace()
            .text(color)
            .with("@color/${projectColor.name}")
            .build()
    }

    companion object {
        private val DESCRIPTION =
            "Цвет не из дизайн системы"
        private val EXPLANATION =
            "Используйте цвета из дизайн системы"

        val ISSUE = Issue.create(
            id = "RawColorUsage",
            briefDescription = DESCRIPTION,
            explanation = EXPLANATION,
            category = Category.create("TestCategory", 77),
            priority = 77,
            severity = Severity.WARNING,
            implementation = Implementation(
                ColorUsageDetector::class.java,
                Scope.RESOURCE_FILE_SCOPE
            )
        )
    }

    data class IssueModel(
        val color: String,
        val location: Location
    )

    data class ColorModel(
        val name: String,
        val color: String
    )
}