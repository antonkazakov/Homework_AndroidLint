package ru.otus.homework.lintchecks.model

import com.android.tools.lint.detector.api.Location
import org.w3c.dom.Attr

data class ColorUsage(
    val value: String,
    val attribute: Attr,
    val location: Location
)