package dev.usbharu.tolo_staff

import android.net.Uri
import net.openid.appauth.connectivity.ConnectionBuilder
import net.openid.appauth.connectivity.DefaultConnectionBuilder
import java.net.HttpURLConnection
import java.net.URL

internal class LocalDevelopmentConnectionBuilder(
    issuer: Uri,
) : ConnectionBuilder {
    private val allowedHost = issuer.host
    private val allowedPort = issuer.port

    init {
        require(issuer.scheme == "http" && !allowedHost.isNullOrBlank()) {
            "Cleartext OIDC requires a valid Debug origin override"
        }
    }

    override fun openConnection(uri: Uri): HttpURLConnection {
        if (uri.scheme == "https") {
            return DefaultConnectionBuilder.INSTANCE.openConnection(uri)
        }
        require(uri.scheme == "http" && uri.host == allowedHost && uri.port == allowedPort) {
            "Cleartext OIDC endpoint must match the configured local issuer"
        }
        return (URL(uri.toString()).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MILLIS
            readTimeout = TIMEOUT_MILLIS
            instanceFollowRedirects = false
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 15_000
    }
}
