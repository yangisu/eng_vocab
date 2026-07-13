package com.yangi.engvocab.core.text

import org.junit.Assert.assertEquals
import org.junit.Test

class ExpressionNormalizerTest {
    @Test
    fun normalizesCaseAndWhitespace() {
        assertEquals("look forward to", normalizeExpression("  Look   Forward TO  "))
    }

    @Test
    fun usesLocaleIndependentLowercase() {
        assertEquals("i", normalizeExpression("I"))
    }
}

