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

private const val CLASS_COROUTINE_SCOPE = "kotlinx.coroutines.CoroutineScope"
private const val CLASS_COROUTINE_CONTEXT = "kotlin.coroutines.CoroutineContext"
private const val CLASS_JOB = "kotlinx.coroutines.Job"
private const val CLASS_VIEW_MODEL = "androidx.lifecycle.ViewModel"

private const val EXPRESSION_SUPERVISOR_JOB = "SupervisorJob()"
private const val EXPRESSION_NONCANCELLABLE = "NonCancellable"

private const val ARTIFACT_VIEW_MODEL = "androidx.lifecycle:lifecycle-viewmodel-ktx"

private const val RECEIVER_VIEW_MODEL_SCOPE = "viewModelScope"

private const val REPLACEMENT_WITH_CONTEXT = "withContext(Dispatchers.Default)"

private const val REPLACE_LAUNCH = "viewModelScope.launch(NonCancellable)"
private const val REPLACE_ASYNC = "viewModelScope.async(NonCancellable)"

class JobInBuilderDetector : Detector(), Detector.UastScanner {
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

        if (node.receiverType?.canonicalText?.contains(CLASS_COROUTINE_SCOPE) != true) return

        val hasJob = node.valueArguments.find { argument ->
            context.evaluator.inheritsFrom(
                context.evaluator.getTypeClass(argument.getExpressionType()),
                CLASS_COROUTINE_CONTEXT,
                false
            ) || context.evaluator.inheritsFrom(
                context.evaluator.getTypeClass(argument.getExpressionType()),
                CLASS_JOB,
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
            argument.sourcePsi?.text?.contains(EXPRESSION_SUPERVISOR_JOB) == true
        } != null

        val hasNonCancellable = node.valueArguments.find { argument ->
            argument.sourcePsi?.text?.contains(EXPRESSION_NONCANCELLABLE) == true
        } != null

        val inViewModel = hasParentClassAndArtifact(
            context,
            ktClass?.toLightClass()?.superTypes ?: emptyArray(),
            CLASS_VIEW_MODEL,
            ARTIFACT_VIEW_MODEL
        )

        val isChildCoroutine =
            psiElement?.getParentOfType<KtBlockExpression>(true)
                ?.getParentOfType<KtBlockExpression>(true)?.statements?.getOrNull(0)?.firstChild?.text == RECEIVER_VIEW_MODEL_SCOPE

        val fix =
            if (receiverName == RECEIVER_VIEW_MODEL_SCOPE && hasNonCancellable && isChildCoroutine) {
                createChildNonCancellableFix(context.getLocation(node))
            } else if (receiverName == RECEIVER_VIEW_MODEL_SCOPE && hasSupervisorJob && inViewModel) {
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

    private fun createViewModelSupervisorFix(location: Location): LintFix {
        return fix().alternatives(
            fix().replace()
                .range(location)
                .text(EXPRESSION_SUPERVISOR_JOB)
                .with("")
                .build()
        )
    }

    private fun createChildNonCancellableFix(location: Location): LintFix {
        return fix().alternatives(
            fix().replace()
                .range(location)
                .text(REPLACE_LAUNCH)
                .with(REPLACEMENT_WITH_CONTEXT)
                .build(),
            fix().replace()
                .range(location)
                .text(REPLACE_ASYNC)
                .with(REPLACEMENT_WITH_CONTEXT)
                .build()
        )
    }
}