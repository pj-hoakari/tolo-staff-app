package dev.usbharu.tolo_staff

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.AuthorizationServiceDiscovery
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidOidcDiscoveryNormalizerTest {
    @Test
    fun debugEmulatorRewritesLoopbackEndpointsWithoutChangingIssuer() {
        val original = configuration("localhost")

        val normalized = normalizeLocalOidcEndpointsForDebug(
            original,
            Uri.parse("http://10.0.2.2:8080"),
        )

        assertEquals("http://localhost:8080", normalized.discoveryDoc?.issuer)
        assertEquals("http://10.0.2.2:8080/oauth2/authorize", normalized.authorizationEndpoint.toString())
        assertEquals("http://10.0.2.2:8080/oauth2/token", normalized.tokenEndpoint.toString())
        assertEquals("http://10.0.2.2:8080/oauth2/jwks", normalized.discoveryDoc?.jwksUri.toString())
    }

    @Test
    fun disabledNormalizationLeavesDiscoveryUntouched() {
        val original = configuration("localhost")

        val normalized = normalizeLocalOidcEndpointsForDebug(
            original,
            null,
        )

        assertEquals("http://localhost:8080/oauth2/authorize", normalized.authorizationEndpoint.toString())
    }

    @Test
    fun debugOverrideCanUseAnyOrigin() {
        val normalized = normalizeLocalOidcEndpointsForDebug(
            configuration("127.0.0.1"),
            Uri.parse("https://idp.dev.example:8443"),
        )

        assertEquals("https://idp.dev.example:8443/oauth2/authorize", normalized.authorizationEndpoint.toString())
        assertEquals("https://idp.dev.example:8443/oauth2/token", normalized.tokenEndpoint.toString())
    }

    private fun configuration(host: String): AuthorizationServiceConfiguration {
        val base = "http://$host:8080"
        val json = JSONObject()
            .put("issuer", base)
            .put("authorization_endpoint", "$base/oauth2/authorize")
            .put("token_endpoint", "$base/oauth2/token")
            .put("jwks_uri", "$base/oauth2/jwks")
            .put("response_types_supported", listOf("code"))
            .put("subject_types_supported", listOf("public"))
            .put("id_token_signing_alg_values_supported", listOf("RS256"))
        return AuthorizationServiceConfiguration(AuthorizationServiceDiscovery(json))
    }
}
