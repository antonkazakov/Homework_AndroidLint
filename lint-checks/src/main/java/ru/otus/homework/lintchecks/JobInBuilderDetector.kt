package ru.otus.homework.lintchecks

import com.android.tools.lint.detector.api.*
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.*
import org.jetbrains.uast.kotlin.KotlinUBinaryExpression

class JobInBuilderDetector : Detector(), Detector.UastScanner {

    companion object {

        private const val ID = "JobInBuilderUsage"
        private const val BRIEF_DESCRIPTION = "Job / Supervisor Job isn`t allowed"
        private const val EXPLANATION = "Using this considered as anti-pattern"
        private const val COROUTINE_CONTEXT = "kotlin.coroutines.CoroutineContext"

        val ISSUE = Issue.create(
            id = ID,
            briefDescription = BRIEF_DESCRIPTION,
            explanation = EXPLANATION,
            category = Category.LINT,
            priority = 5,
            severity = Severity.WARNING,
            implementation = Implementation(
                JobInBuilderDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            ),
            suppressAnnotations = listOf(ID),
        )
    }

    override fun getApplicableMethodNames(): List<String> = listOf("launch", "async")


    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {

        if (context.evaluator.getTypeClass(node.receiverType)
                ?.hasClass(context, "kotlinx.coroutines.CoroutineScope") == false
        ) return

        for (parentNode in node.valueArguments) {
            val param = context.evaluator.getTypeClass(parentNode.getExpressionType())
            var hasJob = context.evaluator.inheritsFrom(
                param,
                "kotlinx.coroutines.Job",
                false
            )

            if (!hasJob && parentNode is KotlinUBinaryExpression) {
                hasJob = context.evaluator.inheritsFrom(
                    param,
                    COROUTINE_CONTEXT,
                    false
                )
            }

            if (!hasJob) continue

            context.report(
                issue = ISSUE,
                scope = node,
                location = context.getLocation(parentNode),
                message = BRIEF_DESCRIPTION,
                quickfixData = createFix(
                    context = context,
                    node = node,
                    parentNode = parentNode
                )
            )
        }
    }

    private fun createFix(
        context: JavaContext,
        node: UExpression,
        parentNode: UExpression
    ): LintFix? {

        val evaluator = context.evaluator
        val psiClass = evaluator.getTypeClass(node.getExpressionType())
        val isNonCancelableJob = evaluator
            .inheritsFrom(psiClass, "kotlinx.coroutines.NonCancellable", false)
        if (isNonCancelableJob && this.isInCoroutine(parentNode)) {
            return fix()
                .replace()
                .range(context.getLocation(parentNode))
                .text((parentNode as UCallExpression).methodName)
                .with("withContext")
                .build()
        }
        if (node.getContainingUClass()
                ?.javaPsi
                ?.superClass
                ?.qualifiedName == "androidx.lifecycle.ViewModel"
            && context.hasDependencies("androidx.lifecycle:lifecycle-viewmodel-ktx")
            && evaluator.inheritsFrom(
                psiClass,
                "kotlinx.coroutines.Job",
                false
            )
        ) {
            return fix().replace().range(context.getLocation(node)).all().with("").build()
        }

        if (node is KotlinUBinaryExpression && evaluator.inheritsFrom(
                psiClass,
                COROUTINE_CONTEXT,
                false
            )
        ) {
            node.operands.forEach {
                val type = evaluator.getTypeClass(it.getExpressionType())
                if (!evaluator.inheritsFrom(
                        type,
                        "kotlinx.coroutines.CompletableJob",
                        false
                    )
                ) return@forEach
                return fix().replace().range(context.getLocation(node)).all().with("").build()
            }
        }
        return null
    }

    private fun isInCoroutine(
        uElement: UElement?
    ): Boolean {
        return when {
            uElement == null || uElement is UMethod -> false
            uElement is UCallExpression
                    && getApplicableMethodNames().any { it == uElement.methodName } -> true
            else -> this.isInCoroutine(
                uElement.uastParent
            )
        }
    }

    fun PsiClass.hasClass(
        context: JavaContext,
        classPath: String,
    ): Boolean {
        var clazz: PsiClass? = this
        val packageName = classPath.substringBeforeLast(".")
        val className = classPath.substringAfterLast(".")

        while (clazz != null) {
            if (clazz.name == className && context.evaluator.getPackage(clazz)?.qualifiedName == packageName) {
                return true
            } else {
                clazz = clazz.superClass
            }
        }
        return false
    }

    private fun JavaContext.hasDependencies(artifact: String): Boolean {
        val dependencies = this.evaluator.dependencies?.getAll()
        val dependency = dependencies?.firstOrNull {
            it.identifier.substringBeforeLast(":").equals(artifact, true)
        }
        return dependency != null
    }
}
