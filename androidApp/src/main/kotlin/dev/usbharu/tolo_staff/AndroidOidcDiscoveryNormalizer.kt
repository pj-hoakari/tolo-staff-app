package dev.usbharu.tolo_staff

import android.net.Uri
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.AuthorizationServiceDiscovery
import org.json.JSONObject

internal fun normalizeLocalOidcEndpointsForDebug(
    configuration: AuthorizationServiceConfiguration,
    originOverride: Uri?,
): AuthorizationServiceConfiguration {
    val override = originOverride ?: return configuration
    if (override.scheme.isNullOrBlank() || override.host.isNullOrBlank()) return configuration
    val discovery = configuration.discoveryDoc ?: return configuration
    val normalized = JSONObject(discovery.docJson.toString())
    DISCOVERY_URL_FIELDS.forEach { field ->
        val value = normalized.optString(field).takeIf(String::isNotBlank) ?: return@forEach
        val uri = Uri.parse(value)
        if (uri.host in LOOPBACK_HOSTS) {
            normalized.put(
                field,
                uri.buildUpon()
                    .scheme(override.scheme)
                    .encodedAuthority(override.encodedAuthority)
                    .build()
                    .toString(),
            )
        }
    }
    return AuthorizationServiceConfiguration(AuthorizationServiceDiscovery(normalized))
}

private val LOOPBACK_HOSTS = setOf("localhost", "127.0.0.1", "::1")
private val DISCOVERY_URL_FIELDS = setOf(
    "authorization_endpoint",
    "token_endpoint",
    "userinfo_endpoint",
    "jwks_uri",
    "registration_endpoint",
    "revocation_endpoint",
    "introspection_endpoint",
    "end_session_endpoint",
)
