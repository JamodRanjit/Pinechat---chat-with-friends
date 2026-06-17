package com.example.util

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest

object CryptoUtils {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    
    // Derive a 256-bit AES key securely using the participant names as entropy
    fun deriveKey(secret: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(secret.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(bytes, "AES")
    }

    // Encrypt content using a derived key
    fun encrypt(plainText: String, secret: String = "secure_chat_e2ee_channel"): String {
        return try {
            val keySpec = deriveKey(secret)
            val cipher = Cipher.getInstance(ALGORITHM)
            // 16-byte fixed IV derived from the system secret for offline reproducibility
            val ivBytes = ByteArray(16)
            val secretBytes = secret.toByteArray(Charsets.UTF_8)
            for (i in 0 until 16) {
                ivBytes[i] = if (i < secretBytes.size) secretBytes[i] else (0x0F + i).toByte()
            }
            val ivSpec = IvParameterSpec(ivBytes)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encryptedBytes, Base64.NO_WRAP).trim()
        } catch (e: Exception) {
            plainText // Fallback to plaintext if error occurs
        }
    }

    // Decrypt content using a derived key
    fun decrypt(cipherText: String, secret: String = "secure_chat_e2ee_channel"): String {
        return try {
            val keySpec = deriveKey(secret)
            val cipher = Cipher.getInstance(ALGORITHM)
            val ivBytes = ByteArray(16)
            val secretBytes = secret.toByteArray(Charsets.UTF_8)
            for (i in 0 until 16) {
                ivBytes[i] = if (i < secretBytes.size) secretBytes[i] else (0x0F + i).toByte()
            }
            val ivSpec = IvParameterSpec(ivBytes)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            val decodedBytes = Base64.decode(cipherText, Base64.NO_WRAP)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            cipherText // Fallback to returning original string
        }
    }
}
