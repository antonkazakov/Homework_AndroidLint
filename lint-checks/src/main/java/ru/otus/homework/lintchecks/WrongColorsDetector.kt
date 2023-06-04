package ru.otus.homework.lintchecks

import com.android.tools.lint.detector.api.*
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.w3c.dom.Attr
import org.w3c.dom.Element
import ru.otus.homework.lintchecks.model.ColorModel
import ru.otus.homework.lintchecks.model.ColorUsage

@Suppress("UnstableApiUsage")
class WrongColorsDetector : ResourceXmlDetector() {

    private val paletteColors = ArrayList<ColorModel>()
    private val allColorUsages = ArrayList<ColorUsage>()

    override fun getApplicableAttributes(): Collection<String> {
        return listOf("color", "tint", "fillColor", "background", "backgroundTint")
    }

    override fun getApplicableElements(): Collection<String> {
        return listOf("color")
    }

    override fun beforeCheckRootProject(context: Context) {
        paletteColors.clear()
        allColorUsages.clear()
    }

    override fun visitAttribute(context: XmlContext, attribute: Attr) {
        val value = attribute.value.lowercase()
        if (value.startsWith("#") || value.contains("color/")) {
            val location = context.getValueLocation(attribute)
            val data = ColorUsage(value, attribute, location)
            allColorUsages.add(data)
        }
    }

    override fun visitElement(context: XmlContext, element: Element) {
        if (context.file.path.contains("/res/values/colors.xml")) {
            paletteColors.add(
                ColorModel(
                    colorName = element.attributes.item(0).nodeValue.toLowerCaseAsciiOnly(),
                    colorValue = element.firstChild.nodeValue.toLowerCaseAsciiOnly()
                )
            )
        }
    }

    override fun afterCheckRootProject(context: Context) {

        allColorUsages.forEach { colorUsages ->

            val colorsReferences = paletteColors.map {
                    model -> "@color/${model.colorName}"
            }
            val rawColorsUsages = paletteColors.find {
                    model ->  colorUsages.value == model.colorValue
            }

            if(rawColorsUsages != null){
                context.report(
                    issue = ISSUE,
                    location = colorUsages.location,
                    message = "Colors outside design system should not be used",
                    quickfixData = createFix("@color/${rawColorsUsages.colorName}", colorUsages.location)
                )
            }

            if(colorsReferences.find { it == colorUsages.value } == null){
                context.report(
                    issue = ISSUE,
                    location = colorUsages.location,
                    message = "Colors outside design system should not be used",
                    quickfixData = null
                )
            }
        }

        paletteColors.clear()
        allColorUsages.clear()
    }

    private fun createFix(replaceTo: String, location: Location): LintFix {
        return fix().replace().range(location).all().with(replaceTo).build()
    }

    companion object {

        private val IMPLEMENTATION = Implementation(
            WrongColorsDetector::class.java,
            Scope.RESOURCE_FILE_SCOPE
        )

        val ISSUE: Issue = Issue
            .create(
                id = "WrongColorUsage",
                briefDescription = "Colors outside design system should not be used",
                explanation = "Colors outside design system should not be used",
                category = Category.CORRECTNESS,
                priority = 10,
                severity = Severity.ERROR,
                androidSpecific = true,
                implementation = IMPLEMENTATION
            )
    }

}