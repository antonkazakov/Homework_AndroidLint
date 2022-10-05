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
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.visitor.AbstractUastVisitor

@Suppress("UnstableApiUsage")
class JobInBuilderDetector : Detector(), Detector.UastScanner {

    companion object {
        private const val MESSAGE =
            "Использование внутри корутин-билдеров не имеет никакого эффекта, это может сломать ожидаемые обработку ошибок и механизм отмены корутин"

        val ISSUE = Issue.create(
            id = "JobInBuilderUsage",
            briefDescription = MESSAGE,
            explanation = "...",
            category = Category.CORRECTNESS,
            priority = 6,
            severity = Severity.ERROR,
            implementation = Implementation(JobInBuilderDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }

    override fun getApplicableMethodNames(): List<String> {
        return listOf("launch", "async")
    }

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (context.evaluator.inheritsFrom(
                context.evaluator.getTypeClass(node.receiverType),
                "kotlinx.coroutines.CoroutineScope",
                false
            )) {
            node.valueArguments.forEach {
                if (it !is ULambdaExpression) {
                    it.accept(
                        MethodArgumentsVisitor(context, node, fix(), getApplicableMethodNames())
                    )
                }
            }
        }
    }

    private class MethodArgumentsVisitor(
        private val context: JavaContext,
        private val parentNode: UCallExpression,
        private val lintFix: LintFix.Builder,
        private val applicableCoroutineBuilderMethodNames: List<String>
    ) : AbstractUastVisitor() {

        override fun visitElement(node: UElement): Boolean {
            if (node is USimpleNameReferenceExpression) {

                if (context.evaluator.inheritsFrom(context.evaluator.getTypeClass(node.getExpressionType()), "kotlinx.coroutines.Job", false)) {
                    val location = context.getLocation(node)

                    context.report(
                        ISSUE,
                        node,
                        location,
                        MESSAGE,
                        getFix(
                            location,
                            context,
                            parentNode,
                            node
                        )
                    )
                }
            }
            return false
        }

        private fun getFix(
            location: Location,
            context: JavaContext,
            parentNode: UCallExpression,
            node: USimpleNameReferenceExpression
        ): LintFix? {
            val parameterName = node.sourcePsi?.text.orEmpty()
            val parent = node.getParentOfType(UClass::class.java)?.javaPsi?.superClass

            return when {
                parameterName.contains("SupervisorJob") && parentNode.receiver?.sourcePsi?.text == "viewModelScope"
                    && parentNode.receiverType?.canonicalText == "kotlinx.coroutines.CoroutineScope"
                    && parent?.qualifiedName.equals("androidx.lifecycle.ViewModel")-> createSupervisorJobFix(location)
                node.getExpressionType()?.canonicalText == "kotlinx.coroutines.NonCancellable" &&
                    isCoroutineBuilderInvokeInOtherCoroutineBuilder(parentNode.uastParent) -> createNonCancellableFix(context.getLocation(parentNode.methodIdentifier))
                else -> null
            }
        }

        private tailrec fun isCoroutineBuilderInvokeInOtherCoroutineBuilder(
            uElement: UElement?
        ): Boolean {
            return when {
                uElement == null || uElement is UMethod -> false
                uElement is UCallExpression
                    && applicableCoroutineBuilderMethodNames.any { it == uElement.methodName } -> true
                else -> isCoroutineBuilderInvokeInOtherCoroutineBuilder(
                    uElement.uastParent
                )
            }
        }

        private fun createSupervisorJobFix(location: Location): LintFix =
            lintFix
                .replace()
                .range(location)
                .all()
                .with("")
                .build()

        private fun createNonCancellableFix(location: Location): LintFix =
            lintFix
                .replace()
                .range(location)
                .all()
                .with("withContext")
                .build()
    }
}