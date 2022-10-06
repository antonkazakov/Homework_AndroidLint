package ru.otus.homework.lintchecks

import com.android.resources.ResourceFolderType
import com.android.tools.lint.detector.api.*
import org.w3c.dom.Attr

@Suppress("UnstableApiUsage")
class RawColorsDetector : ResourceXmlDetector() {

    override fun getApplicableAttributes(): Collection<String>? {
        return XmlScannerConstants.ALL
    }

    override fun appliesTo(folderType: ResourceFolderType): Boolean {
        return folderType in setOf(
            ResourceFolderType.DRAWABLE,
            ResourceFolderType.LAYOUT,
            ResourceFolderType.MIPMAP
        )
    }

    override fun visitAttribute(context: XmlContext, attribute: Attr) {
        val value = attribute.value.orEmpty()
        if (value.matches(COLOR_PATTERN)) {
            context.report(ISSUE, context.getLocation(attribute), BRIEF)
        }
    }

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
            implementation = Implementation(RawColorsDetector::class.java, Scope.RESOURCE_FILE_SCOPE)
        )

    }

}