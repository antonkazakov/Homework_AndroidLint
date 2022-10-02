package ru.otus.homework.lintchecks

import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.model.LintModelLibrary
import org.jetbrains.uast.UClass

const val VIEW_MODEL = "androidx.lifecycle.ViewModel"
const val VIEW_MODEL_ARTIFACT = "androidx.lifecycle:lifecycle-viewmodel-ktx"
const val FRAGMENT = "androidx.fragment.app.Fragment"
const val FRAGMENT_ARTIFACT = "androidx.lifecycle:lifecycle-runtime-ktx"
const val VIEW_MODEL_SCOPE = "viewModelScope"
const val LIFECYCLE_SCOPE = "lifecycleScope"


fun isContainsInClassPath(dependencies: List<LintModelLibrary>?, artifact: String) =
    dependencies?.any { library ->
        library.identifier.contains(artifact)
    } ?: false

fun isExtendsOfClass(
    context: JavaContext,
    containingUClass: UClass?,
    expectedExtendClass: String
): Boolean = context
    .evaluator
    .extendsClass(
        containingUClass,
        expectedExtendClass,
        false
    )