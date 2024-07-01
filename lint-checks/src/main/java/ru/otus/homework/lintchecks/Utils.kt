package ru.otus.homework.lintchecks

import com.android.tools.lint.detector.api.JavaContext
import com.intellij.psi.PsiClass
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.getParentOfType

internal fun <T: UElement> T.isHasParent(
    classPath: String
): Boolean {
    val clazzUElement = this.getParentOfType(UClass::class.java)
    return clazzUElement?.uastSuperTypes?.any { it.getQualifiedName() == classPath } != null
}

internal fun JavaContext.isHasDependency(artifact : String) =
    this.evaluator.dependencies
        ?.getAll()
        ?.firstOrNull { it.identifier.equals(artifact, true) } != null

internal fun PsiClass?.findClass(
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

internal fun PsiClass?.isHasParent(
    context: JavaContext,
    classPath: String,
): Boolean = findClass(context, classPath) != null

internal fun JavaContext.inheritsFrom(
    clazz: String,
    node: UExpression,
): Boolean = evaluator.inheritsFrom(evaluator.getTypeClass(node.getExpressionType()), clazz, false)

internal fun UElement?.isInsideCoroutine(methods: List<String>): Boolean =
    when (val parent = this?.uastParent) {

        is UCallExpression -> methods.any { methodName ->
            methodName == parent.methodName
        }

        null -> false

        else -> parent.isInsideCoroutine(methods)
    }