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
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.w3c.dom.Attr
import org.w3c.dom.Element

const val RAW_COLOR_USAGE_ISSUE_ID = "RawColorUsage"
const val RAW_COLOR_USAGE_BRIEF_DESCRIPTION =
    "BRIEF_DESCRIPTION"
const val RAW_COLOR_USAGE_EXPLANATION =
    "EXPLANATION"

@Suppress("UnstableApiUsage")
class RawColorUsageDetector : ResourceXmlDetector() {

    companion object {
        val ISSUE = Issue.create(
            id = RAW_COLOR_USAGE_ISSUE_ID,
            briefDescription = RAW_COLOR_USAGE_BRIEF_DESCRIPTION,
            explanation = RAW_COLOR_USAGE_EXPLANATION,
            implementation = Implementation(
                RawColorUsageDetector::class.java,
                Scope.RESOURCE_FILE_SCOPE
            ),
            category = Category.PERFORMANCE,
            severity = Severity.FATAL
        )
    }

    private val paletteColors = mutableSetOf<ColorsEntity>()
    private val findingColorUsages = mutableSetOf<FindingColorUsages>()

    private val palateFileName = "/res/values/colors.xml"

    private val setOfAttributes = setOf(
        "color",
        "tint",
        "fillColor",
        "background",
        "backgroundTint",
    )

    private val elements = setOf("color")

    override fun getApplicableAttributes(): Collection<String> = setOfAttributes

    override fun getApplicableElements(): Collection<String> = elements

    override fun beforeCheckRootProject(context: Context) {
        paletteColors.clear()
        findingColorUsages.clear()
    }

    override fun visitAttribute(context: XmlContext, attribute: Attr) {
        val attributeValue = attribute.value.toLowerCaseAsciiOnly()
        if (attributeValue.startsWith("#") || attributeValue.contains("color/")) {
            val location = context.getValueLocation(attribute)
            val data = FindingColorUsages(attributeValue, attribute, location)
            findingColorUsages.add(data)
        }
    }

    override fun visitElement(context: XmlContext, element: Element) {
        if (context.file.path.contains(palateFileName)) {
            val nameOfColor = element.attributes.item(0).nodeValue.toLowerCaseAsciiOnly()
            val valueOfColor = element.firstChild.nodeValue.toLowerCaseAsciiOnly()
            val colorsEntity = ColorsEntity(nameOfColor, valueOfColor)
            paletteColors.add(colorsEntity)
        }
    }

    override fun afterCheckRootProject(context: Context) {
        findingColorUsages.forEach { colorUsages ->
            val colorsEntityReferenceValues = paletteColors.map {
                "@color/${it.name}"
            }
            val paletteColorRawUsages = paletteColors.find { colorUsages.value == it.value }
            val paletteColorReferenceUsages = colorsEntityReferenceValues.find {
                it == colorUsages.value
            } == null

            when {
                paletteColorRawUsages != null -> {
                    report(
                        context,
                        colorUsages.location,
                        "@color/${paletteColorRawUsages.name}"
                    )
                }
                paletteColorReferenceUsages -> {
                    report(
                        context,
                        colorUsages.location,
                        null
                    )
                }
            }
        }

        paletteColors.clear()
        findingColorUsages.clear()
    }

    private fun report(
        context: Context,
        location: Location,
        newValue: String?
    ) {
        context.report(
            ISSUE,
            location,
            "Using color not from palette",
            if (newValue != null) createFix(newValue, location) else null
        )
    }

    private fun createFix(
        newValue: String,
        location: Location
    ): LintFix =
        fix()
            .replace()
            .range(location)
            .all()
            .with(newValue)
            .build()

    private data class ColorsEntity(
        val name: String,
        val value: String
    )

    private data class FindingColorUsages(
        val value: String,
        val attribute: Attr,
        val location: Location
    )
}