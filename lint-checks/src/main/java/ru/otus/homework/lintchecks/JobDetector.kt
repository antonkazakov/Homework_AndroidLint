package ru.otus.homework.lintchecks

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.USimpleNameReferenceExpression

@Suppress("UnstableApiUsage")
class JobDetector : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> {
        return listOf<Class<out UElement>>(UExpression::class.java)
    }

    override fun getApplicableMethodNames(): List<String> {
        return listOf("launch")
    }

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {

            override fun visitExpression(node: UExpression) {
                val expType = node.getExpressionType()
                val type = context.evaluator.getTypeClass(expType)
                val isInherits = context.evaluator.inheritsFrom(type, "kotlinx.coroutines.Job")
                if (isInherits) {
                    val value = node.javaPsi
                    context.report(
                        issue = ISSUE,
                        scope = node,
                        location = context.getLocation(node),
                        message = "asdffdas"
                    )
                }
            }

//            override fun visitCallExpression(node: UCallExpression) {
//                if (node.methodIdentifier?.name == "launch") {
//                    val expType = node.valueArguments[0].getExpressionType()
//                    val type = context.evaluator.getTypeClass(expType)
//                    val isInherits = context.evaluator.inheritsFrom(type, "kotlinx.coroutines.Job")
//                    val value = node.valueArguments[0].asCall()?.methodIdentifier
//                    println()
//                }
//            }

        }
    }

    companion object {

        private const val BRIEF = "brief description"
        private const val EXPLANATION = "explanation"
        private const val ID = "JobDetectorUsage"

        val ISSUE = Issue.create(
            id = ID,
            briefDescription = BRIEF,
            explanation = EXPLANATION,
            category = Category.LINT,
            priority = 3,
            severity = Severity.ERROR,
            implementation = Implementation(JobDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )

    }

}