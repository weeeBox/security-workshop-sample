package co.temy.securitysample.encryption

import android.util.Base64
import java.security.Key
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

/**
 * This class wraps default [Cipher] class apis with some additional possibilities.
 *
 * @throws RuntimeException if there is no algorithm defined with [transformation]
 * @throws RuntimeException if there is no padding defined with [transformation]
 */
class CipherWrapper(val transformation: String) {

    val cipher: Cipher

    companion object {
        /**
         * For default created asymmetric keys
         */
        var TRANSFORMATION_ASYMMETRIC = "RSA/ECB/PKCS1Padding"

        /**
         * For default created symmetric keys
         */
        var TRANSFORMATION_SYMMETRIC = "AES/CBC/PKCS7Padding"

        private val IV_SEPARATOR = "]"
    }

    init {
        cipher = createCipher(transformation)
    }

    /**
     * Encrypts data using IV vector.
     *
     * Note. Do not [cipher] instance with [android.hardware.fingerprint.FingerprintManager] api. Instead
     */
    fun encrypt(data: String, key: Key): String? {
        var cipher = createCipher(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, key)

        var result = ""
        if (key is SecretKey) {
            val iv = cipher.iv
            val ivString = Base64.encodeToString(iv, Base64.DEFAULT)
            result = ivString + IV_SEPARATOR
        }

        val bytes = cipher.doFinal(data.toByteArray())
        result += Base64.encodeToString(bytes, Base64.DEFAULT)

        return result
    }

    fun decrypt(data: String, key: Key): String {
        var encodedString: String

        var cipher = createCipher(transformation)
        cipher.init(Cipher.DECRYPT_MODE, key)
        if (key is SecretKey) {
            val split = data.split(IV_SEPARATOR.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val ivString = split[0]
            encodedString = split[1]
            val ivSpec = IvParameterSpec(Base64.decode(ivString, Base64.DEFAULT))
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
        } else {
            encodedString = data
            cipher.init(Cipher.DECRYPT_MODE, key)
        }

        val encryptedData = Base64.decode(encodedString, Base64.DEFAULT)
        val bytes = Base64.decode(encryptedData, Base64.DEFAULT)

        return String(cipher.doFinal(bytes))
    }

    private fun createCipher(transformation: String): Cipher {
        try {
            return Cipher.getInstance(transformation)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("Failed to get an instance of KeyGenerator", e)
        } catch (e: NoSuchProviderException) {
            throw RuntimeException("Failed to get an instance of KeyGenerator", e)
        }
    }
}

