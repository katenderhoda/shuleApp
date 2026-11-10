package com.example.shulecard.utils

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Salted PBKDF2 password hashing so credentials never sit in SharedPreferences as plaintext.
 * [verify] also accepts legacy plaintext values so accounts created before hashing was added
 * keep working; [isHashed] lets callers opportunistically upgrade those on next successful login.
 */
object PasswordHasher {

    private const val PREFIX = "pbkdf2"
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16

    fun isHashed(value: String): Boolean = value.startsWith("$PREFIX$")

    fun hash(password: String): String {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val derived = deriveKey(password, salt, ITERATIONS)

        return listOf(
            PREFIX,
            ITERATIONS.toString(),
            Base64.encodeToString(salt, Base64.NO_WRAP),
            Base64.encodeToString(derived, Base64.NO_WRAP)
        ).joinToString("$")
    }

    fun verify(password: String, stored: String): Boolean {
        if (!isHashed(stored)) {
            return password == stored
        }

        val parts = stored.split("$")
        if (parts.size != 4) return false

        return try {
            val iterations = parts[1].toInt()
            val salt = Base64.decode(parts[2], Base64.NO_WRAP)
            val expected = Base64.decode(parts[3], Base64.NO_WRAP)
            val actual = deriveKey(password, salt, iterations)
            actual.contentEquals(expected)
        } catch (_: Exception) {
            false
        }
    }

    private fun deriveKey(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        return factory.generateSecret(spec).encoded
    }
}
