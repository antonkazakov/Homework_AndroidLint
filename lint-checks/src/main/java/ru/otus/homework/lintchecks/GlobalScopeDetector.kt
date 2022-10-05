package ru.otus.homework.lintchecks

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.sourcePsiElement

@Suppress("UnstableApiUsage")
class GlobalScopeDetector : Detector(), SourceCodeScanner {

    companion object {
        private const val MESSAGE =
            "Корутины, запущенные на `kotlinx.coroutines.GlobalScope` нужно контролировать вне скоупа класса, в котором они созданы."

        val ISSUE = Issue.create(
            id = "GlobalScopeUsage",
            briefDescription = MESSAGE,
            explanation = """"
        Контролировать глобальные корутины неудобно, а отсутствие контроля может привести к излишнему использованию ресурсов и утечкам памяти.
        """.trimIndent(),
            category = Category.CORRECTNESS,
            priority = 6,
            severity = Severity.ERROR,
            implementation = Implementation(GlobalScopeDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }

    override fun getApplicableUastTypes(): List<Class<out UElement>> {
        return listOf(UCallExpression::class.java)
    }

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {

            override fun visitCallExpression(node: UCallExpression) {
                if (node.receiverType?.canonicalText.equals("kotlinx.coroutines.GlobalScope") && node.methodName?.equals(
                        "launch"
                    ) == true
                ) {
                    context.report(
                        ISSUE,
                        node,
                        context.getLocation(node),
                        MESSAGE,
                        createFix(context, node)
                    )
                }
            }
        }
    }

    private fun createFix(context: JavaContext, node: UCallExpression): LintFix? {
        val parent = node.getParentOfType(UClass::class.java)?.javaPsi?.superClass
        val dependencies = context.evaluator.dependencies?.compileDependencies //always null
        return if (parent?.qualifiedName.equals("androidx.lifecycle.ViewModel")
            && dependencies?.findLibrary("androidx.lifecycle:lifecycle-viewmodel-ktx")!=null) {
            fix()
                .replace()
                .range(context.getLocation(node.receiver))
                .with("viewModelScope")
                .build()
        } else if (parent?.qualifiedName.equals("androidx.fragment.app.Fragment")
            && dependencies?.findLibrary("androidx.lifecycle:lifecycle-runtime-ktx")!=null) {
            fix()
                .replace()
                .range(context.getLocation(node.receiver))
                .with("lifecycleScope")
                .build()
        } else null
    }
}