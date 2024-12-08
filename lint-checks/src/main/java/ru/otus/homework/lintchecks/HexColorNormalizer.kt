package ru.otus.homework.lintchecks

/**
 * Normalize color to uppercase and full format ARGB.
 * E.g. #rgb extended to #FFRRGGBB.
 */
fun normalizeColor(color: String): String? {
    var c = color.uppercase().trim()
    // Уберем лишние пробелы
    c = c.replace("\\s+".toRegex(), "")
    if (!c.startsWith("#")) return null
    val hex = c.substring(1)

    return when (hex.length) {
        // double each letter + FF
        3 -> { // #RGB -> #FFRRGGBB
            val r = hex[0].toString().repeat(2)
            val g = hex[1].toString().repeat(2)
            val b = hex[2].toString().repeat(2)
            "#FF$r$g$b"
        }

        4 -> { // #ARGB -> #AARRGGBB (double each letter)
            val a = hex[0].toString().repeat(2)
            val r = hex[1].toString().repeat(2)
            val g = hex[2].toString().repeat(2)
            val b = hex[3].toString().repeat(2)
            "#$a$r$g$b"
        }
        // prepend FF
        6 -> "#FF$hex" // #RRGGBB -> #FFRRGGBB

        // full format #AARRGGBB, no changes
        8 -> "#$hex"   // #AARRGGBB как есть
        else -> null
    }
}