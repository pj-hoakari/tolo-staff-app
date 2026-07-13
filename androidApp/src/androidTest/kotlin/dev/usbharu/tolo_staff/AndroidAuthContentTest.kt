package dev.usbharu.tolo_staff

import androidx.activity.ComponentActivity
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import dev.usbharu.tolo_staff.feature.auth.AuthPhase
import dev.usbharu.tolo_staff.feature.auth.AuthTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AndroidAuthContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun signedOutStartsOidcLogin() {
        var clicked = false
        composeRule.setContent { AndroidAuthContent(AuthPhase.SIGNED_OUT) { clicked = true } }

        composeRule.onNodeWithTag("auth_login_button").performClick()

        assertTrue(clicked)
    }

    @Test
    fun progressAndRetryStatesAreExposed() {
        composeRule.setContent { AndroidAuthContent(AuthPhase.EXCHANGING) {} }
        composeRule.onNodeWithTag("auth_progress").assertExists()

        composeRule.setContent { AndroidAuthContent(AuthPhase.ERROR) {} }
        composeRule.onNodeWithTag("auth_retry_button").assertExists()
    }

    @Test
    fun keystoreVaultRoundTripsWithoutPlaintextPreferences() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vault = AndroidAuthVault(context)
        val tokens = AuthTokens("tenant-secret", 2_000_000_000, "refresh-secret", "id-secret", "event-secret", 2_000_000_000, "staff-1")

        vault.clear()
        vault.save(tokens)

        assertEquals(tokens, vault.load())
        val stored = context.getSharedPreferences("oidc_secure_state", Context.MODE_PRIVATE).all.toString()
        assertFalse(stored.contains("tenant-secret"))
        assertFalse(stored.contains("event-secret"))
        vault.clear()
    }
}
