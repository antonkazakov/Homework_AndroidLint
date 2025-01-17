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
import com.android.utils.Pair
import org.w3c.dom.Attr
import org.w3c.dom.Element

@Suppress("UnstableApiUsage")
class RawColorUsage : ResourceXmlDetector() {

    private val allowedColors: MutableSet<String> = HashSet()
    private val colorUsages: MutableList<Pair<String, Location>> = ArrayList()

    private val predefinedColors: MutableMap<String, String> = mutableMapOf()

    override fun getApplicableElements(): Collection<String> {
        return listOf("color")
    }

    override fun getApplicableAttributes(): Collection<String> {
        return mutableListOf("color", "textColor", "background", "tint")
    }

    override fun visitAttribute(context: XmlContext, attribute: Attr) {
        val attrValue = attribute.value.orEmpty()

        if ((attrValue.startsWith("#"))) {
            val color = attrValue.uppercase()
            colorUsages.add(
                Pair.of(color, context.getValueLocation(attribute))
            )
        }
    }

    override fun visitElement(context: XmlContext, element: Element) {
        if (context.file.name == COLOR_RESOURCE)
            element.attributes?.item(0)?.nodeValue?.let {
                predefinedColors[element.firstChild.nodeValue.uppercase()] = it
            }
    }

    override fun afterCheckRootProject(context: Context) {
        colorUsages.forEach { pair ->
            var quickfixData: LintFix? = null
            if (pair.first in predefinedColors.keys) {
                quickfixData = fix()
                    .replace()
                    .range(pair.second)
                    .with("@color/${predefinedColors[pair.first]}")
                    .shortenNames()
                    .reformat(true)
                    .build()
            }

            context.report(
                issue = ISSUE,
                location = pair.second,
                message = "Let's replace this color with a resource",
                quickfixData = quickfixData,
            )

        }
        // clean
        allowedColors.clear()
        predefinedColors.clear()
    }

    companion object {
        const val COLOR_RESOURCE = "colors.xml"

        private const val ID = "RawColorUsage"
        private const val DESCRIPTION = "The colors used must be taken from the palette resources."
        private const val EXPLANATION = "All colors used in application resources must be in the palette."
        private val CATEGORY: Category = Category.LINT
        private const val PRIORITY = 6
        private val SEVERITY = Severity.WARNING
        private val IMPLEMENTATION =
            Implementation(RawColorUsage::class.java, Scope.RESOURCE_FILE_SCOPE)

        @JvmField
        val ISSUE: Issue = Issue.create(
            ID, DESCRIPTION, EXPLANATION, CATEGORY, PRIORITY, SEVERITY, IMPLEMENTATION
        )
    }
}