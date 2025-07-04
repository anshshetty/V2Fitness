package v2.utils

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.SecretKeySpec as HmacKeySpec

object CryptoUtils {
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "GymQRCodeKey"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 16
    private const val SALT_LENGTH = 16

    fun generateSalt(): String {
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    fun encrypt(
        plaintext: String,
        password: String,
        salt: String,
    ): Result<String> =
        try {
            val saltBytes = Base64.decode(salt, Base64.NO_WRAP)
            val key = deriveKey(password, saltBytes)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)

            val iv = cipher.iv
            val encryptedData = cipher.doFinal(plaintext.toByteArray())

            // Combine IV + encrypted data
            val combined = iv + encryptedData
            val encoded = Base64.encodeToString(combined, Base64.NO_WRAP)

            Result.success(encoded)
        } catch (e: Exception) {
            Result.failure(e)
        }

    fun decrypt(
        encryptedData: String,
        password: String,
        salt: String,
    ): Result<String> =
        try {
            val saltBytes = Base64.decode(salt, Base64.NO_WRAP)
            val key = deriveKey(password, saltBytes)

            val combined = Base64.decode(encryptedData, Base64.NO_WRAP)

            // Extract IV and encrypted data
            val iv = combined.sliceArray(0..GCM_IV_LENGTH - 1)
            val encrypted = combined.sliceArray(GCM_IV_LENGTH until combined.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            val decrypted = cipher.doFinal(encrypted)
            Result.success(String(decrypted))
        } catch (e: Exception) {
            Result.failure(e)
        }

    fun generateHMAC(
        data: String,
        key: String,
    ): String {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKey = HmacKeySpec(key.toByteArray(), "HmacSHA256")
        mac.init(secretKey)
        val hmacBytes = mac.doFinal(data.toByteArray())
        return Base64.encodeToString(hmacBytes, Base64.NO_WRAP)
    }

    fun verifyHMAC(
        data: String,
        key: String,
        expectedHmac: String,
    ): Boolean {
        val calculatedHmac = generateHMAC(data, key)
        return calculatedHmac == expectedHmac
    }

    private fun deriveKey(
        password: String,
        salt: ByteArray,
    ): SecretKey {
        // Simple key derivation - in production, use PBKDF2 or similar
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(password.toByteArray())
        digest.update(salt)
        val keyBytes = digest.digest()
        return SecretKeySpec(keyBytes, "AES")
    }

    fun generateSecurePassword(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val random = SecureRandom()
        return (1..32)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }
} 
