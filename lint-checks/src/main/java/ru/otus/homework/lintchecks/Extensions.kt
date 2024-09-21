package ru.otus.homework.lintchecks

import com.android.tools.lint.detector.api.JavaContext
import com.intellij.psi.PsiClass

fun PsiClass.isSubclassOf(
    context: JavaContext,
    className: String,
): Boolean {
    val evaluator = context.evaluator
    return evaluator.extendsClass(this, className, false)
}

fun hasArtifact(context: JavaContext, artifactName: String): Boolean {
    return context.evaluator.dependencies?.getAll()?.any {
        it.identifier.contains(artifactName)
    } == true
}