package com.alexey.minay.checks

fun <T>T.find(iterator: (T) -> T?, predicate: (T) -> Boolean): T? {
    var condition = predicate(this)
    var element = this
    while (!condition) {
        element = iterator(element) ?: return null
        condition = predicate(element)
    }
    return element
}