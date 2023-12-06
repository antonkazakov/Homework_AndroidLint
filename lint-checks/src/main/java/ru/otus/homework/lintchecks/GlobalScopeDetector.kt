package ru.otus.homework.lintchecks

import com.android.tools.lint.detector.api.*
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import java.io.File

private const val ID = "GlobalScopeUsage"
private const val BRIEF_DESCRIPTION = "Не используйте GlobalScope"
private const val EXPLANATION = """Корутины, запущенные на kotlinx.coroutines.GlobalScope нужно контролировать вне 
    |скоупа класса, в котором они созданы. Контролировать глобальные корутины неудобно, а отсутствие контроля может 
    |привести к излишнему использованию ресурсов и утечкам памяти."""

class GlobalScopeDetector : Detector(), SourceCodeScanner {

    companion object {
        val ISSUE = Issue.create(
            id = ID,
            briefDescription = BRIEF_DESCRIPTION,
            explanation = EXPLANATION,
            category = Category.CORRECTNESS,
            priority = 6,
            severity = Severity.WARNING,
            implementation = Implementation(
                GlobalScopeDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }

    override fun getApplicableMethodNames(): List<String> {
        return listOf("launch", "async", "runBlocking")
    }

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (context.evaluator.isMemberInClass(method, "kotlinx.coroutines.GlobalScope")) {
            context.report(
                ISSUE,
                node,
                context.getLocation(node),
                BRIEF_DESCRIPTION,
                quickFix(context, method)
            )
        }
    }

    private fun quickFix(context: JavaContext, method: PsiMethod): LintFix? {
        return if (canReplaceWithViewModelScope(context, method)) {
            fix()
                .name("Заменить GlobalScope на viewModelScope")
                .replace()
                .text("GlobalScope")
                .with("viewModelScope")
                .autoFix()
                .build()
        } else if (canReplaceWithLifecycleScope(context, method)) {
            fix()
                .name("Заменить GlobalScope на lifecycleScope")
                .replace()
                .text("GlobalScope")
                .with("lifecycleScope")
                .autoFix()
                .build()
        } else {
            null
        }
    }

    private fun canReplaceWithViewModelScope(context: JavaContext, method: PsiMethod): Boolean {
        if (context.evaluator.extendsClass(method.containingClass, "androidx.lifecycle.ViewModel")) {
            return false
        }
        val buildGradleFiles = findBuildGradleFiles(context.project.dir)
        return buildGradleFiles.any { it.readText().contains("androidx.lifecycle:lifecycle-viewmodel-ktx") }
    }

    private fun canReplaceWithLifecycleScope(context: JavaContext, method: PsiMethod): Boolean {
        if (context.evaluator.extendsClass(method.containingClass, "androidx.fragment.app.Fragment")) {
            return false
        }
        val buildGradleFiles = findBuildGradleFiles(context.project.dir)
        return buildGradleFiles.any { it.readText().contains("androidx.lifecycle:lifecycle-runtime-ktx") }
    }

    private fun findBuildGradleFiles(directory: File): List<File> {
        val buildGradleFiles = mutableListOf<File>()
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                buildGradleFiles.addAll(findBuildGradleFiles(file))
            } else if (file.name == "build.gradle" || file.name == "build.gradle.kts") {
                buildGradleFiles.add(file)
            }
        }
        return buildGradleFiles
    }
}