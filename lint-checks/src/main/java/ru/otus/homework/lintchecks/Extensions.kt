package ru.otus.homework.lintchecks

import com.android.tools.lint.detector.api.JavaContext
import com.intellij.psi.PsiClass

fun PsiClass.hasSuperClass(superClassName: String): Boolean {
    val superClassQualifiedName = this.superClass?.qualifiedName
    return superClassQualifiedName == superClassName
}

fun PsiClass.hasClass(className: String): Boolean {
    val qualifiedName = this.qualifiedName
    return qualifiedName == className
}


fun JavaContext.hasDependencies(artifact: String): Boolean {
    val dependencies = this.evaluator.dependencies?.getAll()
    val dependency = dependencies?.firstOrNull {
        it.identifier.substringBeforeLast(":").equals(artifact, true)
    }
    return dependency != null
}