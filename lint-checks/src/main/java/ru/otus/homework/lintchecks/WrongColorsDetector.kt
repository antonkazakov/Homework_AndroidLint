package ru.otus.homework.lintchecks

import org.w3c.dom.Attr
import org.w3c.dom.Element
import com.android.resources.ResourceFolderType
import com.android.tools.lint.detector.api.*
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression

@Suppress("UnstableApiUsage")
class WrongColorsDetector : ResourceXmlDetector() {

    override fun appliesTo(folderType: ResourceFolderType): Boolean {

        return folderType == ResourceFolderType.DRAWABLE || folderType == ResourceFolderType.LAYOUT || folderType == ResourceFolderType.VALUES

    }

    override fun visitAttribute(context: XmlContext, attribute: Attr) {
        super.visitAttribute(context, attribute)
    }

    private val designColors = ArrayList<Pair<Location, String>>()
    private val allProjectColors = HashMap<String, ArrayList<String>>()

    override fun visitElement(context: XmlContext, element: Element) {
        if (context.file.path.contains("colors.xml")){

            val color = element.firstChild.nodeValue.lowercase()
            val name = element.attributes.item(0)?.nodeValue?.lowercase() ?: return

        }

    }

    override fun beforeCheckRootProject(context: Context) {
        designColors.clear()
        allProjectColors.clear()
    }

    private fun reportUsage(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        context.report(
            issue = ISSUE,
            scope = node,
            location = context.getLocation(node),
            message = "Using Job/SupervisorJob in a builder makes no sense",
            //quickfixData = fix
        )
    }

    companion object {

        private val IMPLEMENTATION = Implementation(
            UselessJobDetector::class.java,
            Scope.JAVA_FILE_SCOPE
        )

        val ISSUE: Issue = Issue
            .create(
                id = "WrongColorsUsage",
                briefDescription = "The Job in a coroutine builder should not be used",
                explanation = """
                Using Job/SupervisorJob in a builder makes no sense.
            """.trimIndent(),
                category = Category.CORRECTNESS,
                priority = 10,
                severity = Severity.ERROR,
                androidSpecific = true,
                implementation = IMPLEMENTATION
            )
    }

}