package com.yangi.engvocab.core.text

import java.util.Locale

private val repeatedWhitespace = Regex("\\s+")

fun normalizeExpression(value: String): String =
    value.trim().lowercase(Locale.ROOT).replace(repeatedWhitespace, " ")
