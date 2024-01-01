package ru.otus.homework.lintchecks

import com.android.tools.lint.detector.api.JavaContext
import com.intellij.psi.PsiClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.getContainingUClass

fun PsiClass.hasClass(
context: JavaContext,
classPath: String,
): Boolean {
    return findClass(context, classPath) != null
}

fun UElement.hasClass(
    context: JavaContext,
    classPath: String,
): Boolean {
    val psiClass = getContainingUClass()?.javaPsi ?: return false

    return psiClass.hasClass(context, classPath)
}

private fun PsiClass.findClass(
    context: JavaContext,
    classPath: String,
): PsiClass? {
    var clazz: PsiClass? = this
    val (classPackage, className) = with(classPath) {
        substringBeforeLast(".") to substringAfterLast(".")
    }

    while (clazz != null) {
        val isTargetClassName = clazz.name == className
        val isTargetClassPackage =
            context.evaluator.getPackage(clazz)?.qualifiedName == classPackage

        if (isTargetClassName && isTargetClassPackage) return clazz

        clazz = clazz.superClass
    }

    return null
}