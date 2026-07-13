package com.yangi.engvocab.core.security

data class EncryptedValue(
    val ciphertext: ByteArray,
    val iv: ByteArray,
)

interface ApiKeyCipher {
    fun encrypt(plaintext: ByteArray): EncryptedValue

    fun decrypt(value: EncryptedValue): ByteArray
}
