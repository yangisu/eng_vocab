package com.yangi.engvocab.core.study

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerGraderTest {
    @Test
    fun ignoresCaseAndRepeatedWhitespace() {
        assertTrue(gradeTypedAnswer("Look forward to", " look   FORWARD to "))
    }

    @Test
    fun keepsPunctuationSignificant() {
        assertFalse(gradeTypedAnswer("don't", "dont"))
    }
}

