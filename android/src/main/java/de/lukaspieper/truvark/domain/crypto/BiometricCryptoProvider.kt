/*
 * SPDX-FileCopyrightText: 2024 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.domain.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties.*
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

/**
 * Provider for secure encrypting and decrypting [BiometricPrompt.CryptoObject] featuring AES GCM and user
 * authentication backed by the Android [KeyStore].
 */
class BiometricCryptoProvider(context: Context) {

    companion object {
        private const val BIOMETRIC_KEY_ALIAS = "BIOMETRIC_ACCESS_KEY"

        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "$KEY_ALGORITHM_AES/$BLOCK_MODE_GCM/$ENCRYPTION_PADDING_NONE"
        private const val TAG_LENGTH = 128
    }

    private val biometricManager by lazy { BiometricManager.from(context) }
    private val keyStore by lazy { KeyStore.getInstance(ANDROID_KEY_STORE).also { it.load(null) } }

    /**
     * Checks if the device supports biometric authentication. Returns an *AuthenticationStatus* as defined in
     * [BiometricManager].
     */
    fun checkBiometricSupport(): Int {
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
    }

    @Throws(Exception::class)
    fun createEncryptingPromptObject(): BiometricPrompt.CryptoObject {
        // If the user changes the biometric settings in the Android settings (lock screen), the key becomes invalid
        // but is not removed, therefore the key is always overwritten during biometric setup.
        if (keyStore.containsAlias(BIOMETRIC_KEY_ALIAS)) {
            keyStore.deleteEntry(BIOMETRIC_KEY_ALIAS)
        }

        with(KeyGenerator.getInstance(KEY_ALGORITHM_AES, ANDROID_KEY_STORE)) {
            val keyGenParameterSpec = KeyGenParameterSpec
                .Builder(BIOMETRIC_KEY_ALIAS, PURPOSE_ENCRYPT or PURPOSE_DECRYPT)
                .setBlockModes(BLOCK_MODE_GCM)
                .setEncryptionPaddings(ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(true)
                .build()

            init(keyGenParameterSpec)
            generateKey()
        }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keyStore.getKey(BIOMETRIC_KEY_ALIAS, null))
        return BiometricPrompt.CryptoObject(cipher)
    }

    @Throws(Exception::class)
    fun createDecryptingPromptObject(iv: ByteArray): BiometricPrompt.CryptoObject {
        check(keyStore.containsAlias(BIOMETRIC_KEY_ALIAS)) { "KeyStore does not contain required key." }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            keyStore.getKey(BIOMETRIC_KEY_ALIAS, null),
            GCMParameterSpec(TAG_LENGTH, iv)
        )
        return BiometricPrompt.CryptoObject(cipher)
    }
}
