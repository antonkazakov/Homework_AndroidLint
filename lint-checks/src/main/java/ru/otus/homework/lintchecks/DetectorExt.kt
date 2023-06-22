package ru.otus.homework.lintchecks

import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.JavaContext
import com.intellij.psi.PsiType

fun Detector.hasParentClassAndArtifact(
    context: JavaContext,
    superTypes: Array<out PsiType>,
    canonicalClassName: String,
    artifact: String
): Boolean {
    superTypes.forEach {
        val hasDependency = context.evaluator.dependencies?.getAll()
            ?.any { it.identifier.lowercase().contains(artifact) } == true
        return if (it.canonicalText == canonicalClassName && hasDependency) {
            true
        } else {
            hasParentClassAndArtifact(
                context,
                it.superTypes ?: emptyArray(),
                canonicalClassName,
                artifact
            )
        }
    }
    return false
}