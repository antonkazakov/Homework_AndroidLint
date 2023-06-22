package ru.otus.homework.lintchecks

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.uast.UCallExpression

private const val ID = "JobInBuilderUsage"
private const val BRIEF_DESCRIPTION =
    "Удалите экземпляр Job из coroutine builder"
private const val EXPLANATION =
    "Удалите экземпляр Job из coroutine builder. " +
            "Bспользование Job внутри корутин-билдеров не имеет никакого эффекта, это может сломать ожидаемые обработку ошибок и механизм отмены корутин"

private const val PRIORITY = 6

private const val CLASS = "kotlinx.coroutines.CoroutineScope"

class JobInBuilderDetector: Detector(), Detector.UastScanner {
    companion object {

        val ISSUE = Issue.create(
            ID,
            BRIEF_DESCRIPTION,
            EXPLANATION,
            Category.create("TEST CATEGORY", 10),
            PRIORITY,
            Severity.WARNING,
            Implementation(JobInBuilderDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }

    override fun getApplicableMethodNames() = listOf("async", "launch")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        super.visitMethodCall(context, node, method)

        if (node.receiverType?.canonicalText?.contains(CLASS) != true) return

        val hasJob = node.valueArguments.find { argument ->
            context.evaluator.inheritsFrom(
                context.evaluator.getTypeClass(argument.getExpressionType()),
                "kotlin.coroutines.CoroutineContext",
                false
            ) || context.evaluator.inheritsFrom(
                context.evaluator.getTypeClass(argument.getExpressionType()),
                "kotlinx.coroutines.Job",
                false
            )
        } != null

        if (!hasJob) {
            return
        }

        val psiElement = node.sourcePsi

        val ktClass = psiElement?.getParentOfType<KtClass>(true)

        val receiverName = node.receiver?.asSourceString() ?: ""

        val hasSupervisorJob = node.valueArguments.find { argument ->
            argument.sourcePsi?.text?.contains("SupervisorJob()") == true
        } != null

        val hasNonCancellable = node.valueArguments.find { argument ->
            argument.sourcePsi?.text?.contains("NonCancellable") == true
        } != null

        val inViewModel = hasParentClassAndArtifact(
            context,
            ktClass?.toLightClass()?.superTypes ?: emptyArray(),
            "androidx.lifecycle.ViewModel",
            "androidx.lifecycle:lifecycle-viewmodel-ktx"
        )

        val isChildCoroutine =
            psiElement?.getParentOfType<KtBlockExpression>(true)
                ?.getParentOfType<KtBlockExpression>(true)?.statements?.getOrNull(0)?.firstChild?.text == "viewModelScope"

        val fix = if (receiverName == "viewModelScope" && hasNonCancellable && isChildCoroutine) {
            createChildNonCancellableFix(context.getLocation(node))
        } else if (receiverName == "viewModelScope" && hasSupervisorJob && inViewModel) {
            createViewModelSupervisorFix(context.getLocation(node))
        } else {
            null
        }

        context.report(
            ISSUE,
            node,
            context.getLocation(node),
            BRIEF_DESCRIPTION,
            fix
        )
    }

    private fun hasParentClassAndArtifact(
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

    private fun createViewModelSupervisorFix(location: Location): LintFix {
        return fix().alternatives(
            fix().replace()
                .range(location)
                .text("SupervisorJob()")
                .with("")
                .build()
        )
    }

    private fun createChildNonCancellableFix(location: Location): LintFix {
        return fix().alternatives(
            fix().replace()
                .range(location)
                .text("viewModelScope.launch(NonCancellable)")
                .with("withContext(Dispatchers.Default)")
                .build(),
            fix().replace()
                .range(location)
                .text("viewModelScope.async(NonCancellable)")
                .with("withContext(Dispatchers.Default)")
                .build()
        )
    }
}