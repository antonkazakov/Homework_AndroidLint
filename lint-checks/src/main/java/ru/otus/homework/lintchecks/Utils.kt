package ru.otus.homework.lintchecks

import com.android.tools.lint.detector.api.JavaContext
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiType


internal fun isSubtypeOf(
    context: JavaContext, psiType: PsiType?, superClassName: String
): Boolean {
    return psiType?.let { type ->
        context.evaluator.getTypeClass(type)?.inheritsFrom(context, superClassName) == true
    } ?: false
}

private fun PsiClass.inheritsFrom(context: JavaContext, className: String): Boolean =
    context.evaluator.inheritsFrom(this, className, false)

internal fun isDependencyPresent(context: JavaContext, dependency: String): Boolean {
    return context.evaluator.dependencies?.getAll()?.any { dep ->
        dep.identifier.contains(dependency)
    } ?: false
}