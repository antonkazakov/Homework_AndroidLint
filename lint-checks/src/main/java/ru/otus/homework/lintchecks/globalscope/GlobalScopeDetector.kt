package ru.otus.homework.lintchecks.globalscope

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.JavaContext
import org.jetbrains.uast.UElement
import org.jetbrains.uast.USimpleNameReferenceExpression

@Suppress("UnstableApiUsage")
class GlobalScopeDetector : Detector(), Detector.UastScanner {
    override fun getApplicableUastTypes(): List<Class<out UElement>> =
        listOf(USimpleNameReferenceExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        GlobalScopeVisitor(context)
}
