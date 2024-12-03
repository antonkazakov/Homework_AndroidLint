package ru.otus.homework.lintchecks

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.kotlin.KotlinUFunctionCallExpression
import org.jetbrains.uast.kotlin.toPsiType

@Suppress("UnstableApiUsage")
class JobInBuilderUsageDetector : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> {
        return listOf(UCallExpression::class.java)
    }

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {

            override fun visitCallExpression(node: UCallExpression) {
                if (node !is KotlinUFunctionCallExpression) return

                val methodName = node.methodName ?: return
                if (methodName != "launch" && methodName != "async") return

                val arguments = node.valueArguments
                for (arg in arguments) {
                    val type = arg.getExpressionType() ?: continue
                    if (
                        type.canonicalText == "kotlinx.coroutines.Job" ||
                        type.canonicalText == "kotlinx.coroutines.SupervisorJob" ||
                        type.canonicalText == "kotlinx.coroutines.NonCancellable"
                    ) {
                        val enclosingClass: PsiType? =
                            node.sourcePsi?.getParentOfType<KtClass>(true)?.toPsiType()
                        val fix = createFix(context, type, enclosingClass)

                        context.report(
                            issue = ISSUE,
                            scope = node,
                            location = context.getLocation(node),
                            message = BRIEF_DESCRIPTION,
                            quickfixData = fix
                        )
                    }
                }
            }

            private fun createFix(context: JavaContext, type: PsiType, enclosingClass: PsiType?): LintFix? {
                return when {
                    type.canonicalText == "kotlinx.coroutines.SupervisorJob" && isDependencyPresent(
                        context,
                        DEPENDENCY_LIFECYCLE_VIEW_MODEL_KTX
                    ) && isEnclosingClassSubclassOf(context, enclosingClass, VIEW_MODEL_FULL_CLASS_NAME) ->
                        removeSupervisorJobFix()

                    type.canonicalText == "kotlinx.coroutines.NonCancellable" ->
                        replaceWithContextFix()

                    else -> null
                }
            }

            private fun removeSupervisorJobFix(): LintFix {
                return fix().replace()
                    .text("SupervisorJob()")
                    .with("")
                    .build()
            }

            private fun replaceWithContextFix(): LintFix {
                return fix().replace()
                    .text("launch")
                    .with("withContext(NonCancellable)")
                    .build()
            }

            private fun isEnclosingClassSubclassOf(
                context: JavaContext,
                psiType: PsiType?,
                superClassName: String
            ): Boolean {
                return psiType?.let { type ->
                    context.evaluator.getTypeClass(type)?.extendsClass(context, superClassName) == true
                } ?: false
            }

            private fun PsiClass.extendsClass(context: JavaContext, className: String): Boolean {
                return context.evaluator.extendsClass(this, className, false)
            }

            private fun isDependencyPresent(context: JavaContext, dependency: String): Boolean {
                return context.evaluator.dependencies?.getAll()?.any { dep ->
                    dep.identifier.contains(dependency)
                } ?: false
            }
        }
    }

    companion object {
        private const val ID = "JobInBuilderUsage"
        private const val BRIEF_DESCRIPTION = "Don't use Job in builders"
        private const val EXPLANATION = """
            Using Job in builder can lead to:
            - Ошибки перестанут правильно распространяться: дочерние корутины могут работать "вне контроля".
            - Обработка отмены сломается: корутины не будут завершаться, как ожидается.
        """

        private const val VIEW_MODEL_FULL_CLASS_NAME = "androidx.lifecycle.ViewModel"
        private const val DEPENDENCY_LIFECYCLE_VIEW_MODEL_KTX = "androidx.lifecycle:lifecycle-viewmodel-ktx"

        val ISSUE: Issue = Issue.create(
            id = ID,
            briefDescription = BRIEF_DESCRIPTION,
            explanation = EXPLANATION,
            category = Category.CORRECTNESS,
            priority = 6,
            severity = Severity.ERROR,
            implementation = Implementation(
                JobInBuilderUsageDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }
}