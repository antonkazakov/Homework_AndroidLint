package ru.otus.homework.lintchecks.color

import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.ResourceXmlDetector
import com.android.tools.lint.detector.api.XmlContext
import org.jetbrains.uast.UElement
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.w3c.dom.Attr
import org.w3c.dom.Element
import ru.otus.homework.lintchecks.color.ColorIssue.ISSUE

@Suppress("UnstableApiUsage")
class ColorDetector : ResourceXmlDetector() {
    private val colorsFileName = "/res/values/colors.xml"
    private val allowedColors = mutableListOf<String>()
    private val usedColors = mutableListOf<Pair<Attr, Location>>()

    override fun beforeCheckRootProject(context: Context) {
        allowedColors.clear()
        usedColors.clear()
    }

    override fun afterCheckRootProject(context: Context) {
        usedColors.forEach { (attr, location) ->
            val content = attr.textContent.substringAfter("@color/")
            if (!allowedColors.contains(content)) {
                reportIssue(context, location)
            }
        }
        allowedColors.clear()
        usedColors.clear()
    }

    override fun getApplicableUastTypes(): List<Class<out UElement>> =
        listOf(USimpleNameReferenceExpression::class.java)

    override fun getApplicableAttributes(): Collection<String> {
        return listOf("color", "tint", "fillColor", "background", "backgroundTint")
    }

    override fun getApplicableElements(): Collection<String> {
        return listOf("color")
    }

    override fun visitElement(context: XmlContext, element: Element) {
        if (context.file.path.contains(colorsFileName)) {
            val nameOfColor = element.attributes.item(0).nodeValue
            val valueOfColor = element.firstChild.nodeValue
            allowedColors.add(nameOfColor)
            allowedColors.add(valueOfColor)
            return
        }
    }

    override fun visitAttribute(context: XmlContext, attribute: Attr) {
        val content = attribute.textContent
        if (content.startsWith("@color/") || content.startsWith("@android:color/") || content.startsWith("#")) {
            val location = context.getValueLocation(attribute)
            usedColors.add(Pair(attribute, location))
        }
    }

    private fun reportIssue(context: Context, location: Location) {
        context.report(
            issue = ISSUE,
            location = location,
            message = ColorIssue.BRIEF_DESCRIPTION
        )
    }
}
