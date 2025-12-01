package com.ledger.app.data.local.db

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages the database encryption passphrase using Android Keystore.
 * 
 * The passphrase is generated once and stored encrypted in SharedPreferences.
 * The encryption key is stored in Android Keystore, which provides hardware-backed
 * security on supported devices.
 * 
 * Requirements: 7.2 - Data stored in local encrypted Room database
 */
class DatabasePassphraseManager(private val context: Context) {

    companion object {
        private const val KEYSTORE_ALIAS = "ledger_db_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val PREFS_NAME = "ledger_db_prefs"
        private const val ENCRYPTED_PASSPHRASE_KEY = "encrypted_passphrase"
        private const val IV_KEY = "passphrase_iv"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val PASSPHRASE_LENGTH = 32 // 256 bits
    }

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    /**
     * Gets or creates the database passphrase.
     * 
     * If a passphrase already exists, it is decrypted and returned.
     * If no passphrase exists, a new one is generated, encrypted, and stored.
     * 
     * @return The passphrase as a CharArray for use with SQLCipher
     */
    fun getOrCreatePassphrase(): CharArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedPassphrase = prefs.getString(ENCRYPTED_PASSPHRASE_KEY, null)
        val ivString = prefs.getString(IV_KEY, null)

        return if (encryptedPassphrase != null && ivString != null) {
            // Decrypt existing passphrase
            decryptPassphrase(encryptedPassphrase, ivString)
        } else {
            // Generate and store new passphrase
            generateAndStorePassphrase()
        }
    }


    /**
     * Generates a new random passphrase, encrypts it, and stores it.
     */
    private fun generateAndStorePassphrase(): CharArray {
        // Generate a random passphrase
        val passphrase = generateRandomPassphrase()
        
        // Ensure the keystore key exists
        if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
            createKeystoreKey()
        }
        
        // Encrypt and store the passphrase
        val secretKey = keyStore.getKey(KEYSTORE_ALIAS, null) as SecretKey
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        
        val encryptedBytes = cipher.doFinal(passphrase.toString().toByteArray(Charsets.UTF_8))
        val iv = cipher.iv
        
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(ENCRYPTED_PASSPHRASE_KEY, Base64.encodeToString(encryptedBytes, Base64.NO_WRAP))
            .putString(IV_KEY, Base64.encodeToString(iv, Base64.NO_WRAP))
            .apply()
        
        return passphrase
    }

    /**
     * Decrypts the stored passphrase using the keystore key.
     */
    private fun decryptPassphrase(encryptedPassphrase: String, ivString: String): CharArray {
        val secretKey = keyStore.getKey(KEYSTORE_ALIAS, null) as SecretKey
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val iv = Base64.decode(ivString, Base64.NO_WRAP)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        
        val encryptedBytes = Base64.decode(encryptedPassphrase, Base64.NO_WRAP)
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        
        return String(decryptedBytes, Charsets.UTF_8).toCharArray()
    }

    /**
     * Creates a new AES key in the Android Keystore.
     */
    private fun createKeystoreKey() {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false) // Allow access without user authentication
            .build()
        
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    /**
     * Generates a cryptographically secure random passphrase.
     */
    private fun generateRandomPassphrase(): CharArray {
        val secureRandom = java.security.SecureRandom()
        val bytes = ByteArray(PASSPHRASE_LENGTH)
        secureRandom.nextBytes(bytes)
        // Convert to Base64 for a printable passphrase
        return Base64.encodeToString(bytes, Base64.NO_WRAP).toCharArray()
    }

    /**
     * Clears the stored passphrase. Use with caution - this will make
     * the existing database inaccessible!
     */
    fun clearPassphrase() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(ENCRYPTED_PASSPHRASE_KEY)
            .remove(IV_KEY)
            .apply()
        
        // Remove the keystore key
        if (keyStore.containsAlias(KEYSTORE_ALIAS)) {
            keyStore.deleteEntry(KEYSTORE_ALIAS)
        }
    }

    /**
     * Checks if a passphrase has been generated and stored.
     */
    fun hasPassphrase(): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains(ENCRYPTED_PASSPHRASE_KEY) && prefs.contains(IV_KEY)
    }
}
