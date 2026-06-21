package com.tippingpoint.pedastudio.util

/** Like [String.format] but treats dynamic args as literals (safe if they contain `%`). */
fun String.formatSafe(vararg args: Any?): String {
    if (args.isEmpty()) return this
    val escaped = args.map { arg ->
        arg?.toString()?.replace("%", "%%") ?: ""
    }.toTypedArray()
    return String.format(this, *escaped)
}
