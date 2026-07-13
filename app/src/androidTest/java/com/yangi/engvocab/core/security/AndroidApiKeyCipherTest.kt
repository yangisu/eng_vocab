package com.yangi.engvocab.core.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidApiKeyCipherTest {
    private val cipher = AndroidApiKeyCipher(alias = "eng_vocab_test_key_${System.nanoTime()}")

    @Test
    fun encryptsWithRandomIvAndDecryptsBothValues() {
        val plaintext = "sk-test-secret".encodeToByteArray()

        val first = cipher.encrypt(plaintext)
        val second = cipher.encrypt(plaintext)

        assertFalse(first.iv.contentEquals(second.iv))
        assertArrayEquals(plaintext, cipher.decrypt(first))
        assertArrayEquals(plaintext, cipher.decrypt(second))
    }

    @Test
    fun ciphertextDoesNotContainPlaintext() {
        val plaintext = "sk-visible-value".encodeToByteArray()

        val encrypted = cipher.encrypt(plaintext)

        assertFalse(encrypted.ciphertext.contentEquals(plaintext))
    }
}
