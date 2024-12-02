package ru.otus.homework.lintchecks


import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.kotlin.toPsiType

private const val ID = "GlobalScopeUsage"
private const val BRIEF_DESCRIPTION = "Don't use GlobalScope for coroutines"
private const val EXPLANATION = """
    Using GlobalScope can lead to coroutines that outlive the lifecycle of your app components, \
    potentially causing memory leaks and excessive resource usage. Prefer using a more appropriate scope \
    such as viewModelScope or lifecycleScope.
"""

private const val PRIORITY = 6

private const val CLASS = "kotlinx.coroutines.GlobalScope"
//private const val CLASS = "GlobalScope"

private const val VIEW_MODEL_FULL_CLASS_NAME = "androidx.lifecycle.ViewModel"
private const val FRAGMENT_FULL_CLASS_NAME = "androidx.fragment.app.Fragment"


class GlobalScopeUsageDetector : Detector(), Detector.UastScanner {

    override fun getApplicableMethodNames(): List<String> = listOf("launch", "async", "runBlocking", "actor")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        super.visitMethodCall(context, node, method)

        if (node.receiverType?.canonicalText?.contains(CLASS) != true) return

        val psiElement = node.sourcePsi

        val className = psiElement?.getParentOfType<KtClass>(true)?.name
        val methodCallExpression = node.methodIdentifier?.sourcePsi

        val enclosingClass: KtClass? = psiElement?.getParentOfType<KtClass>(true)

        val psiParentClass: PsiClass? = context.evaluator.getTypeClass(enclosingClass!!.toPsiType())

        context.report(
            issue = ISSUE,
            scope = node,
            location = context.getLocation(node.receiver),
            message = BRIEF_DESCRIPTION,
            quickfixData = createFix(context, enclosingClass)
        )
    }

    private fun createFix(context: JavaContext, enclosingClass: KtClass?): LintFix? {
        if (isEnclosingClassSubclassOf(context, enclosingClass, VIEW_MODEL_FULL_CLASS_NAME)) {
            return replaceWithViewModelScope()
        }
        else if (isEnclosingClassSubclassOf(context, enclosingClass, FRAGMENT_FULL_CLASS_NAME)) {
            return replaceWithLifecycleScope()
        }
        return null
    }

    private fun isEnclosingClassSubclassOf(
        context: JavaContext, enclosingClass: KtClass?, superClassName: String
    ): Boolean {
        if (enclosingClass != null) {
            val psiClass = context.evaluator.getTypeClass(enclosingClass.toPsiType())
            if (psiClass != null && psiClass.extendsClass(context, superClassName)) {
                return true
            }
        }
        return false
    }


    private fun PsiClass.extendsClass(context: JavaContext, className: String): Boolean =
        context.evaluator.extendsClass(this, className, false)


    private fun replaceWithViewModelScope(): LintFix {
        return fix().replace()
            .text("GlobalScope")
            .with("viewModelScope")
            .build()
    }

    private fun replaceWithLifecycleScope(): LintFix {
        return fix().replace()
            .text("GlobalScope")
            .with("lifecycleScope")
            .build()
    }

    companion object {
        val ISSUE: Issue = Issue.create(
            id = ID,
            briefDescription = BRIEF_DESCRIPTION,
            explanation = EXPLANATION,
            category = Category.CORRECTNESS,
            priority = 6,
            severity = Severity.WARNING,
            implementation = Implementation(
                GlobalScopeUsageDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }
}
