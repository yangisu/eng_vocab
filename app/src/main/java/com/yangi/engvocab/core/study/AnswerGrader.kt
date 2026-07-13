package com.yangi.engvocab.core.study

import com.yangi.engvocab.core.text.normalizeExpression

fun gradeTypedAnswer(expected: String, actual: String): Boolean =
    normalizeExpression(expected) == normalizeExpression(actual)
