@file:Suppress("UnstableApiUsage")

package ru.otus.homework.lintchecks

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LayoutDetector
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.XmlContext
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UImportStatement
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.getContainingUFile
import org.jetbrains.uast.getParentOfType
import org.w3c.dom.Element

class GlobalScopeDetector : Detector(), Detector.UastScanner {

    override fun getApplicableReferenceNames(): List<String>? {
        return listOf("GlobalScope")
    }

    override fun visitReference(
        context: JavaContext,
        reference: UReferenceExpression,
        referenced: PsiElement
    ) {
        super.visitReference(context, reference, referenced)

        if (reference.getExpressionType()?.canonicalText == "kotlinx.coroutines.GlobalScope") {
            val parentClass = reference.getParentOfType<UClass>()
            if (context.evaluator.extendsClass(parentClass, "androidx.fragment.app.Fragment")) {
                context.report(
                    issue = ISSUE,
                    scope = reference,
                    location = context.getLocation(reference),
                    message = DESCRIPTION,
                    quickfixData = createFragmentFix(
                        lastImportLocation = getLastImportLocationIfNotExists(
                            context = context,
                            reference = reference,
                            import = IMPORT_LIFECYCLE_SCOPE
                        )
                    )
                )
            } else if (context.evaluator.extendsClass(
                    parentClass,
                    "androidx.lifecycle.ViewModel"
                )
            ) {
                context.report(
                    issue = ISSUE,
                    scope = reference,
                    location = context.getLocation(reference),
                    message = DESCRIPTION,
                    quickfixData = createViewModelFix(
                        lastImportLocation = getLastImportLocationIfNotExists(
                            context = context,
                            reference = reference,
                            import = IMPORT_VIEWMODEL_SCOPE
                        )
                    )
                )
            }
        }
    }

    private fun getLastImportLocationIfNotExists(
        context: JavaContext,
        reference: UReferenceExpression,
        import: String
    ): Location? {
        val imports = reference.getParentOfType<UFile>()?.imports
        return if (imports?.firstOrNull { it.sourcePsi?.text == import } == null) {
            return context.getLocation(imports?.lastOrNull())
        } else {
            null
        }
    }

    private fun createFragmentFix(lastImportLocation: Location?): LintFix {
        val fix = LintFix.create().composite()

        val changeScopeFix = LintFix.create()
            .replace()
            .text(GLOBAL_SCOPE)
            .with("lifecycleScope")
            .build()
        fix.add(changeScopeFix)

        if (lastImportLocation != null) {
            val addImportFix = LintFix.create()
                .replace()
                .with("\n$IMPORT_LIFECYCLE_SCOPE")
                .range(lastImportLocation)
                .end()
                .build()
            fix.add(addImportFix)
        }

        return fix.build()
    }

    private fun createViewModelFix(lastImportLocation: Location?): LintFix {
        val fix = LintFix.create().composite()

        val changeScopeFix = LintFix.create()
            .replace()
            .text(GLOBAL_SCOPE)
            .with("viewModelScope")
            .build()
        fix.add(changeScopeFix)

        if (lastImportLocation != null) {
            val addImportFix = LintFix.create()
                .replace()
                .with("\n$IMPORT_VIEWMODEL_SCOPE")
                .range(lastImportLocation)
                .end()
                .build()
            fix.add(addImportFix)
        }

        return fix.build()
    }

    companion object {
        private val DESCRIPTION =
            "Не используйте GlobalScope."
        private val EXPLANATION =
            "Использование GlobalScope может приводить к утечкам памяти."

        val ISSUE = Issue.create(
            id = "GlobalScopeUsage",
            briefDescription = DESCRIPTION,
            explanation = EXPLANATION,
            category = Category.create("TestCategory", 77),
            priority = 7,
            severity = Severity.WARNING,
            implementation = Implementation(GlobalScopeDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )

        private const val IMPORT_LIFECYCLE_SCOPE = "import androidx.lifecycle.lifecycleScope"
        private const val IMPORT_VIEWMODEL_SCOPE = "import androidx.lifecycle.viewModelScope"
        private const val GLOBAL_SCOPE = "GlobalScope"
    }
}