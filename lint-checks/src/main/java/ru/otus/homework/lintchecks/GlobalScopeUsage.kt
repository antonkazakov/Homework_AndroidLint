package ru.otus.homework.lintchecks

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Detector.UastScanner
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiClass
import com.intellij.psi.util.InheritanceUtil
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.uast.UElement
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.evaluateString
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.getQualifiedParentOrThis
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.uast.UClass

@Suppress("UnstableApiUsage")
class GlobalScopeUsage : Detector(), UastScanner {
    override fun getApplicableUastTypes(): List<Class<out UElement>> {
        return listOf<Class<out UElement>>(USimpleNameReferenceExpression::class.java)
    }

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {

            override fun visitSimpleNameReferenceExpression(node: USimpleNameReferenceExpression) {
                if (node.identifier == "GlobalScope") {
                    context.report(
                        issue = ISSUE,
                        location = context.getLocation(node),
                        message = "Let's change 'GlobalScope' to another type 'CoroutineScope'",
                        quickfixData = createFix(context, node)
                    )
                }
            }
        fun createFix(context: JavaContext, node: USimpleNameReferenceExpression): LintFix? {
            //androidx.lifecycle.ViewModel
            //androidx.lifecycle:lifecycle-viewmodel-ktx
            if(checkParentClass(
                    node.getContainingUClass(),
                    "ViewModel",
                    "androidx.lifecycle"
            )) {
                return fix()
                    .replace()
                    .range(context.getLocation(node))
                    .with("viewModelScope")
                    .shortenNames()
                    .reformat(true)
                    .build()
            }

            //androidx.fragment.app.Fragment
            //androidx.lifecycle:lifecycle-runtime-ktx
            if(checkParentClass(
                    node.getContainingUClass(),
                    "Fragment",
                    "androidx.lifecycle"
                )) {
                return fix()
                    .replace()
                    .range(context.getLocation(node))
                    .with("viewLifecycleOwner.lifecycleScope")
                    .shortenNames()
                    .reformat(true)
                    .build()
            }
            return null
        }
            fun checkParentClass(uClass: UClass?, className: String, packageName:String): Boolean {
                var psiClass = uClass?.javaPsi
                while(!(psiClass == null || psiClass.name == className))
                    psiClass = psiClass.superClass
                return psiClass!=null &&
                        context.evaluator.getPackage(psiClass)?.qualifiedName == packageName
            }
        }
    }

    companion object {
        @JvmField
        val ISSUE: Issue =
            Issue.create(
                id = "GlobalScopeUsage",
                briefDescription = "Try to avoid using GlobalScope, replace this",
                explanation =
                """
                Coroutines running on kotlinx.coroutines.GlobalScope need to be controlled outside the scope
                of the class they are created in.
                """,
                category = Category.LINT,
                priority = 6,
                severity = Severity.WARNING,
                implementation = Implementation(GlobalScopeUsage::class.java, Scope.JAVA_FILE_SCOPE),
            )
    }
}