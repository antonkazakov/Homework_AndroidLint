package ru.otus.homework.lintchecks

import com.android.tools.lint.detector.api.JavaContext
import com.intellij.psi.PsiClass

fun PsiClass.findClass(
    context: JavaContext,
    classPath: String
): PsiClass? {
    var currentClass: PsiClass? = this
    val (classPackage, className) = classPath.substringBeforeLast('.') to classPath.substringAfterLast('.')

    while (currentClass != null) {
        val isMatchingClassName = currentClass.name == className
        val isMatchingClassPackage =
            context.evaluator.getPackage(currentClass)?.qualifiedName == classPackage

        if (isMatchingClassName && isMatchingClassPackage) {
            return currentClass
        }

        currentClass = currentClass.superClass
    }

    return null
}
