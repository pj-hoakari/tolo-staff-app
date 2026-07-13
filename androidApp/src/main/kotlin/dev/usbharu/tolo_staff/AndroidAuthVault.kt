package dev.usbharu.tolo_staff

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dev.usbharu.tolo_staff.feature.auth.AuthTokens
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal class AndroidAuthVault(context: Context) {
    private val preferences = context.getSharedPreferences("oidc_secure_state", Context.MODE_PRIVATE)

    fun save(tokens: AuthTokens) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val plaintext = JSONObject()
            .put("tenantAccessToken", tokens.tenantAccessToken)
            .put("tenantExpiresAt", tokens.tenantExpiresAtEpochSeconds)
            .put("refreshToken", tokens.refreshToken)
            .put("idToken", tokens.idToken)
            .put("eventAccessToken", tokens.eventAccessToken)
            .put("eventExpiresAt", tokens.eventExpiresAtEpochSeconds)
            .put("subject", tokens.subject)
            .toString().toByteArray()
        preferences.edit()
            .putString("ciphertext", Base64.encodeToString(cipher.doFinal(plaintext), Base64.NO_WRAP))
            .putString("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .apply()
    }

    fun load(): AuthTokens? = runCatching {
        val ciphertext = preferences.getString("ciphertext", null) ?: return null
        val iv = preferences.getString("iv", null) ?: return null
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
        val json = JSONObject(String(cipher.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP))))
        AuthTokens(
            tenantAccessToken = json.getString("tenantAccessToken"),
            tenantExpiresAtEpochSeconds = json.getLong("tenantExpiresAt"),
            refreshToken = json.optString("refreshToken").takeIf { it.isNotBlank() && it != "null" },
            idToken = json.optString("idToken").takeIf { it.isNotBlank() && it != "null" },
            eventAccessToken = json.getString("eventAccessToken"),
            eventExpiresAtEpochSeconds = json.getLong("eventExpiresAt"),
            subject = json.getString("subject"),
        )
    }.getOrNull()

    fun clear() { preferences.edit().clear().apply() }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build())
            generateKey()
        }
    }

    private companion object {
        const val KEY_ALIAS = "tolo_staff_oidc_tokens"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
