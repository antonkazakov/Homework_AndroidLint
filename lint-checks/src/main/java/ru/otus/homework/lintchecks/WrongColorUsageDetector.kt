package ru.otus.homework.lintchecks

import com.android.resources.ResourceFolderType
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
import com.android.tools.lint.detector.api.XmlScannerConstants
import org.w3c.dom.Attr
import org.w3c.dom.Element
import java.util.regex.Pattern

@Suppress("UnstableApiUsage")
internal class WrongColorUsageDetector : ResourceXmlDetector() {

    private data class ColorInfo(
        val location: Location,
        val colorValue: String,
        val context: XmlContext,
        val element: Element,
        val attributeName: String,
        val attributeValue: String
    )

    private val hexColorsAndRefs = mutableListOf<ColorInfo>()

    // e.g. #FF018786 -> teal_700
    private val colorValuesToNamesPalette = mutableMapOf<String, String>()

    override fun getApplicableAttributes(): Collection<String>? {
        return XmlScannerConstants.ALL
    }

    override fun getApplicableElements(): Collection<String> {
        return XmlScannerConstants.ALL
    }

    override fun visitAttribute(context: XmlContext, attribute: Attr) {
        val attrValue = attribute.value.orEmpty()
        if (isHexColor(attrValue) || isAndroidColorReference(attrValue)) {
//            normalizeColor(attrValue)?.let {
                hexColorsAndRefs.add(
                    ColorInfo(
                        location = context.getValueLocation(attribute),
//                        colorValue = it,
                        colorValue = attrValue,
                        context = context,
                        element = attribute.ownerElement,
                        attributeName = attribute.name,
                        attributeValue = attrValue
                    )
                )
//            }
            println("hexColors: ${hexColorsAndRefs.map { it.colorValue }}")
        }
    }

    private fun isHexColor(value: String): Boolean {
        return COLOR_PATTERN.matcher(value).matches()
    }

    override fun visitElement(context: XmlContext, element: Element) {
        val resourceFolderType = context.resourceFolderType ?: return

        // save colors from colors.xml for further reference
        if (resourceFolderType == ResourceFolderType.VALUES &&
            context.file.name == PALETTE_RESOURCE_FILE) {
            if (saveColorToMap(element)) return

        }
//        else {
//            val attributes: NamedNodeMap = element.attributes
//            for (i in 0 until attributes.length) {
//                val attr: Node = attributes.item(i)
//                val attrName = attr.localName ?: continue
//                val attrValue = attr.nodeValue?.trim() ?: continue
//
//                // Проверяем, выглядит ли значение как цвет
//                // Может быть:
//                //   1) Hex цвет (#RRGGBB или #AARRGGBB)
//                //   2) Ссылка на ресурс цвета (@color/...), @android:color/...
//                //   Ссылка вида @color/teal_700 - это ок, она уже из палитры или нет - проверим потом.
//                //   Нас интересуют сырые цвета (#FF000000), а также @android:color, т.к. они не в палитре проекта.
//
//                // Если @color/... (без android), это скорее всего палитра или другой цвет из проекта
//                // Если @android:color/... - это системный цвет, который не в палитре.
//                // Если #... - это сырой цвет
//                if (isHexColor(attrValue) || isAndroidColorReference(attrValue)) {
//                    hexColors += ColorToLocation(
//                        colorValue = attrValue,
//                        context = context,
//                        element = element,
//                        attributeName = attrName,
//                        attributeValue = attrValue,
//                        location = context.getValueLocation(attr as Attr)
//                    )
//                }
//            }
//        }
    }

    private fun saveColorToMap(element: Element): Boolean {
        val name = element.getAttribute("name")
        val colorTextValue = element.textContent?.trim() ?: return true

        normalizeColor(colorTextValue)?.let { normalized ->
            colorValuesToNamesPalette[normalized.uppercase()] = name
        }
        return false
    }

    private fun isAndroidColorReference(value: String): Boolean {
        return value.startsWith("@android:color/")
    }

    override fun afterCheckRootProject(context: Context) {

        // ------------------- works ----
//        hexColors.forEach { hexColor ->
//            val colorNameToReplace = colorValuesToNamesPalette[hexColor.colorValue]
//            val location = hexColor.location
//            val fix = colorNameToReplace?.let {
//                quickColorFix(location = location, newColor = it)
//            }
//            context.report(
//                issue = ISSUE,
//                location = location,
//                message = BRIEF_DESCRIPTION,
//                quickfixData = fix
//            )
//        }

        // -------------------

//        for (info in hexColors) {
//            val rawValue = info.colorValue
//
//            if (rawValue.startsWith("#")) {
//                val location = info.context.getValueLocation(info.element.getAttributeNode(info.attributeName))
//                val fix = colorValuesToNamesPalette[rawValue.uppercase()]?.let {
//                    quickColorFix(location = location, newColor = it)
//                }
//                context.report(
//                    issue = ISSUE,
//                    location = location,
//                    message = BRIEF_DESCRIPTION,
//                    quickfixData = fix
//                )
//            }
//        }
        // -------------------

        for (info in hexColorsAndRefs) {
            val rawValue = info.colorValue

            if (rawValue.startsWith("#")) {
                // Это сырой HEX цвет
                val normalized = normalizeColor(rawValue)
                if (normalized != null) {
                    val colorName = colorValuesToNamesPalette[normalized.uppercase()]
                    if (colorName != null) {
                        // Есть совпадение в палитре
                        // Предлагаем фикс: заменить сырой цвет на @color/colorName
//                        val fix = LintFix.create()
//                            .replace()
//                            .name("Заменить на @color/$colorName")
//                            .text(rawValue)
//                            .with("@color/$colorName")
//                            .autoFix()
//                            .build()

                        val fix = LintFix.create()
                            .replace()
                            .range(info.location)
//                            .text(rawValue)
                            .with("")
                            .build()

                        info.context.report(
                            issue = WrongColorUsageDetector.ISSUE,
                            scope = info.element,
//                            location = info.context.getLocation(info.element.getAttributeNode(info.attributeName)),
                            location = info.location,
//                            message = "Цвет $rawValue есть в палитре, используйте @color/$colorName вместо сырого цвета.",
                            message = BRIEF_DESCRIPTION,
                            quickfixData = fix
                        )

                    } else {

                        val fix = LintFix.create()
                            .replace()
                            .range(info.location)
//                            .text(rawValue)
                            .with("")
                            .build()

                        // Нет совпадения в палитре
                        info.context.report(
                            issue = WrongColorUsageDetector.ISSUE,
                            scope = info.element,
//                            location = info.context.getLocation(info.element.getAttributeNode(info.attributeName)),
                            location = info.location,
//                            message = "Используется сырой цвет $rawValue, отсутствующий в палитре. Добавьте его в палитру или используйте существующий цвет."
                            message = BRIEF_DESCRIPTION,
                            quickfixData = fix

                        )
                    }
                }
            } else if (rawValue.startsWith("@android:color/")) {

                val fix = LintFix.create()
                    .replace()
                    .range(info.location)
//                            .text(rawValue)
                    .with("")
                    .build()

                // Системный цвет - не из палитры
                info.context.report(
                    issue = WrongColorUsageDetector.ISSUE,
                    scope = info.element,
//                    location = info.context.getLocation(info.element.getAttributeNode(info.attributeName)),
                    location = info.location,
//                    message = "Используется системный цвет $rawValue, которого нет в палитре. Добавьте соответствующий цвет в палитру или используйте уже имеющийся."
                    message = BRIEF_DESCRIPTION,
                    quickfixData = fix
                )
            }
        }

    }

    private fun quickColorFix(location: Location, newColor: String): LintFix {
        return fix()
            .replace()
            .range(location)
            .all()
            .with(COLOR_PREFIX + newColor)
            .build()
    }

    companion object {

        // matches hexadecimal colors of lengths 3, 4, 6, or 8
        private val COLOR_PATTERN = Pattern.compile("^#(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$")

        private const val ID = "WrongColorUsage"
        private const val BRIEF_DESCRIPTION = "Should use colors only from palette"
        private const val EXPLANATION =
            "All app colors should be taken from the color palette defined in `colors.xml`"
        private const val PRIORITY = 6
        private const val PALETTE_RESOURCE_FILE = "colors.xml"
        private const val COLOR_PREFIX = "@color/"

        val ISSUE = Issue.create(
            id = ID,
            briefDescription = BRIEF_DESCRIPTION,
            explanation = EXPLANATION,
            category = Category.CORRECTNESS,
            priority = PRIORITY,
            severity = Severity.WARNING,
            implementation = Implementation(
                WrongColorUsageDetector::class.java,
                Scope.RESOURCE_FILE_SCOPE
            )
        )
    }
}