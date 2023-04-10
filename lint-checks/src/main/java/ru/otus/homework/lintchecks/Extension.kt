package ru.otus.homework.lintchecks

import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.model.LintModelLibrary
import com.intellij.psi.PsiClass
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.getContainingUClass

fun JavaContext.getLintModelLibrary(
    artifact: String
): LintModelLibrary? = evaluator.dependencies
    ?.getAll()
    ?.firstOrNull { it.identifier.equals(artifact, true) }

fun PsiClass.findClass(
    context: JavaContext,
    classPath: String,
): PsiClass? {
    var clazz: PsiClass? = this
    val classPackage = classPath.substringBeforeLast(".")
    val className = classPath.substringAfterLast(".")

    while (clazz != null) {
        if (clazz.name == className && context.evaluator.getPackage(clazz)?.qualifiedName == classPackage) {
            return clazz
        } else {
            clazz = clazz.superClass
        }
    }

    return null
}

fun PsiClass.hasClass(
    context: JavaContext,
    classPath: String,
): Boolean = findClass(context, classPath) != null

fun UElement.findClass(
    context: JavaContext,
    classPath: String,
): PsiClass? = getContainingUClass()?.javaPsi?.findClass(context, classPath)

fun UElement.hasClass(
    context: JavaContext,
    classPath: String,
): Boolean = findClass(context, classPath) != null

fun JavaContext.inheritsFrom(
    clazz: String,
    node: UExpression,
): Boolean = evaluator.inheritsFrom(evaluator.getTypeClass(node.getExpressionType()), clazz, false)

fun UElement?.isInsideCoroutine(methods: List<String>): Boolean =
    when (val parent = this?.uastParent) {

        is UCallExpression -> methods.any { methodName ->
            methodName == parent.methodName
        }

        null -> false

        else -> parent.isInsideCoroutine(methods)
    }
