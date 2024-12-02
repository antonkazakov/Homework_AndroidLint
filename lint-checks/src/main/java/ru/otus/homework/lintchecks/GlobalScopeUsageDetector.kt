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
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.kotlin.toPsiType


class GlobalScopeUsageDetector : Detector(), Detector.UastScanner {

    override fun getApplicableMethodNames(): List<String> = listOf("launch", "async", "runBlocking", "actor")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        super.visitMethodCall(context, node, method)

        if (node.receiverType?.canonicalText?.contains(CLASS) != true) return

        val psiElement = node.sourcePsi
        val enclosingClass: PsiType? = psiElement?.getParentOfType<KtClass>(true)?.toPsiType()

        context.report(
            issue = ISSUE,
            scope = node,
            location = context.getLocation(node.receiver),
            message = BRIEF_DESCRIPTION,
            quickfixData = if (enclosingClass == null) null else createFix(context, enclosingClass)
        )
    }

    private fun createFix(context: JavaContext, enclosingClass: PsiType): LintFix? {
        if (isEnclosingClassSubclassOf(context, enclosingClass, VIEW_MODEL_FULL_CLASS_NAME)) {
            return replaceWithViewModelScope()
        } else if (isEnclosingClassSubclassOf(context, enclosingClass, FRAGMENT_FULL_CLASS_NAME)) {
            return replaceWithLifecycleScope()
        }
        return null
    }

    private fun isEnclosingClassSubclassOf(
        context: JavaContext, psiType: PsiType?, superClassName: String
    ): Boolean {
        if (psiType != null) {
            val psiClass = context.evaluator.getTypeClass(psiType)
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
            .text(FIX_REPLACE_TARGET)
            .with(FIX_REPLACE_WITH_VIEW_MODEL_SCOPE)
            .build()
    }

    private fun replaceWithLifecycleScope(): LintFix {
        return fix().replace()
            .text(FIX_REPLACE_TARGET)
            .with(FIX_REPLACE_WITH_LIFECYCLE_SCOPE)
            .build()
    }

    companion object {
        private const val ID = "GlobalScopeUsage"
        private const val BRIEF_DESCRIPTION = "Don't use GlobalScope for coroutines"
        private const val EXPLANATION = """
    Using GlobalScope can lead to coroutines that outlive the lifecycle of your app components, \
    potentially causing memory leaks and excessive resource usage. Prefer using a more appropriate scope \
    such as viewModelScope or lifecycleScope.
"""

        private const val PRIORITY = 6

        private const val CLASS = "kotlinx.coroutines.GlobalScope"

        private const val VIEW_MODEL_FULL_CLASS_NAME = "androidx.lifecycle.ViewModel"
        private const val FRAGMENT_FULL_CLASS_NAME = "androidx.fragment.app.Fragment"

        private const val FIX_REPLACE_TARGET = "GlobalScope"
        private const val FIX_REPLACE_WITH_LIFECYCLE_SCOPE = "lifecycleScope"
        private const val FIX_REPLACE_WITH_VIEW_MODEL_SCOPE = "viewModelScope"

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
