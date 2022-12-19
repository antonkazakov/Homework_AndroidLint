package ru.otus.homework.lintchecks

import com.android.tools.lint.detector.api.*
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.w3c.dom.Attr
import org.w3c.dom.Element

@Suppress("UnstableApiUsage")
class RawColorsDetector : ResourceXmlDetector() {
    companion object {
        private const val MESSAGE = "Do not use raw colors"
        val ISSUE = Issue.create(
            id = "RawColorUsage",
            briefDescription = "Raw color usage",
            explanation = MESSAGE,
            implementation = Implementation(
                RawColorsDetector::class.java,
                Scope.RESOURCE_FILE_SCOPE
            ),
            category = Category.create("HOMEWORK CATEGORY", 5),
            severity = Severity.ERROR
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
                    context.report(
                        ISSUE,
                        colorUsages.location,
                        MESSAGE,
                        createFix("@color/${paletteColorRawUsages.name}", colorUsages.location)
                    )
                }
                paletteColorReferenceUsages -> {
                    context.report(
                        ISSUE,
                        colorUsages.location,
                        MESSAGE,
                        null
                    )
                }
            }
        }

        paletteColors.clear()
        findingColorUsages.clear()
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