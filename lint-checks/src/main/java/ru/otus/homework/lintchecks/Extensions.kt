package ru.otus.homework.lintchecks

import com.android.tools.lint.detector.api.JavaContext
import com.intellij.psi.PsiClass

fun PsiClass.hasParent(context: JavaContext, parentName: String): Boolean {
    val className = parentName.substringAfterLast(".")
    val classPackage = parentName.substringBeforeLast(".")
    var psiClass = this
    while (psiClass.name != className) {
        psiClass = psiClass.superClass ?: return false
    }
    return context.evaluator.getPackage(psiClass)?.qualifiedName == classPackage
}

fun hasArtifact(context: JavaContext, artifactName: String): Boolean {
    return context.evaluator.dependencies?.getAll()?.any {
        it.identifier.contains(artifactName)
    } == true
}

fun String.isRawColor(): Boolean {
    return this.matches("^#([a-fA-F0-9]{3}|[a-fA-F0-9]{6}|[a-fA-F0-9]{8})$".toRegex())
}