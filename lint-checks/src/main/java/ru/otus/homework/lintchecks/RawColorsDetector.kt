package ru.otus.homework.lintchecks

import com.android.resources.ResourceFolderType
import com.android.tools.lint.detector.api.*
import org.w3c.dom.Attr
import org.w3c.dom.Element

@Suppress("UnstableApiUsage")
class RawColorsDetector : ResourceXmlDetector() {

    private val mColorToNames = mutableMapOf<String, MutableList<String>>()
    private val mRawColors = mutableListOf<RawColor>()

    override fun getApplicableAttributes(): Collection<String>? {
        return XmlScannerConstants.ALL
    }

    override fun getApplicableElements(): Collection<String> {
        return XmlScannerConstants.ALL
    }

    override fun appliesTo(folderType: ResourceFolderType): Boolean {
        return folderType in setOf(
            ResourceFolderType.DRAWABLE,
            ResourceFolderType.LAYOUT,
            ResourceFolderType.MIPMAP,
            ResourceFolderType.VALUES
        )
    }

    override fun visitAttribute(context: XmlContext, attribute: Attr) {
        val value = attribute.value.orEmpty()
        if (value.matches(COLOR_PATTERN)) {
            mRawColors.add(
                RawColor(
                    location = context.getValueLocation(attribute),
                    color = value.lowercase()
                )
            )
        }
    }

    override fun visitElement(context: XmlContext, element: Element) {
        if (context.file.path.contains("colors.xml")) {
            val color = element.firstChild.nodeValue.lowercase()
            if (color.matches(COLOR_PATTERN)) return
            val name = element.attributes.item(0)?.nodeValue?.lowercase() ?: return
            when (mColorToNames[color]) {
                null -> mColorToNames[color] = mutableListOf(name)
                else -> mColorToNames[color]?.add(name)
            }
        }
    }

    override fun beforeCheckRootProject(context: Context) {
        mColorToNames.clear()
        mRawColors.clear()
    }

    override fun afterCheckRootProject(context: Context) {
        mRawColors.forEach { rawColor ->
            val fixValues = mColorToNames[rawColor.color]
            context.report(
                ISSUE,
                rawColor.location,
                BRIEF,
                quickfixData = createFix(fixValues, rawColor.location)
            )
        }
    }

    private fun createFix(
        fixValues: MutableList<String>?,
        location: Location
    ): LintFix? {
        fixValues ?: return null

        val fixes = fixValues.map { value ->
            fix()
                .replace()
                .range(location)
                .all()
                .with(value)
                .build()
        }
        return fix().alternatives(*fixes.toTypedArray())
    }

    data class RawColor(
        val location: Location,
        val color: String
    )

    companion object {
        private const val BRIEF = "brief description"
        private const val EXPLANATION = "explanation"
        private const val ID = "RawColorUsage"
        private val COLOR_PATTERN = "^#([a-fA-F0-9]{6}|[a-fA-F0-9]{3}|[a-fA-F0-9]{8})$".toRegex()

        val ISSUE = Issue.create(
            id = ID,
            briefDescription = BRIEF,
            explanation = EXPLANATION,
            category = Category.LINT,
            priority = 3,
            severity = Severity.ERROR,
            implementation = Implementation(
                RawColorsDetector::class.java,
                Scope.RESOURCE_FILE_SCOPE
            )
        )

    }

}