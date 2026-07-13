package dev.usbharu.tolo_staff

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import dev.usbharu.tolo_staff.feature.auth.AuthFailure
import dev.usbharu.tolo_staff.feature.auth.AuthRepository
import dev.usbharu.tolo_staff.feature.auth.AuthTokens
import dev.usbharu.tolo_staff.runtime.AppRuntimeConfig
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AppAuthConfiguration
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.EndSessionRequest
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenRequest
import net.openid.appauth.connectivity.ConnectionBuilder
import net.openid.appauth.connectivity.DefaultConnectionBuilder
import org.json.JSONObject

internal class AndroidOidcCoordinator(context: Context, private val config: AppRuntimeConfig, private val repository: AuthRepository) {
    private val issuer = Uri.parse(config.oidcIssuer)
    private val debugOriginOverride = BuildConfig.TOLO_OIDC_DEBUG_ORIGIN_OVERRIDE
        .takeIf { BuildConfig.DEBUG && it.isNotBlank() }
        ?.let(Uri::parse)
    private val networkIssuer = debugOriginOverride?.let { origin ->
        issuer.buildUpon()
            .scheme(origin.scheme)
            .encodedAuthority(origin.encodedAuthority)
            .build()
    } ?: issuer
    private val allowsLocalCleartext = BuildConfig.DEBUG && networkIssuer.scheme == "http"
    private val connectionBuilder: ConnectionBuilder = if (allowsLocalCleartext) {
        LocalDevelopmentConnectionBuilder(networkIssuer)
    } else {
        DefaultConnectionBuilder.INSTANCE
    }
    private val appAuthConfiguration = AppAuthConfiguration.Builder()
        .setConnectionBuilder(connectionBuilder)
        .setSkipIssuerHttpsCheck(allowsLocalCleartext)
        .build()
    private val service = AuthorizationService(context, appAuthConfiguration)
    private val vault = AndroidAuthVault(context)
    private var serviceConfiguration: AuthorizationServiceConfiguration? = null

    fun restore() {
        val restored = vault.load()
        val now = System.currentTimeMillis() / 1000
        when {
            restored == null -> repository.restore(null, now)
            restored.eventExpiresAtEpochSeconds <= now + 60 -> refresh(restored)
            else -> repository.restore(restored, now)
        }
    }

    fun beginAuthorization(launch: (Intent) -> Unit) {
        repository.startAuthorization()
        AuthorizationServiceConfiguration.fetchFromIssuer(networkIssuer, { discovered, error ->
            if (discovered == null || error != null) {
                repository.fail(AuthFailure.CONFIGURATION)
                return@fetchFromIssuer
            }
            val configuration = normalizeLocalOidcEndpointsForDebug(discovered, debugOriginOverride)
            serviceConfiguration = configuration
            val request = AuthorizationRequest.Builder(configuration, config.oidcClientId, ResponseTypeValues.CODE, Uri.parse(config.oidcRedirectUri))
                .setScopes(config.oidcScopes.split(' ').filter(String::isNotBlank))
                .setAdditionalParameters(mapOf("audience" to config.oidcAudience, "resource" to config.tenantResource()))
                .build()
            launch(service.getAuthorizationRequestIntent(request))
        }, connectionBuilder)
    }

    fun handleAuthorizationResult(data: Intent?) {
        if (data == null) {
            repository.fail(AuthFailure.CANCELLED)
            return
        }
        val response = AuthorizationResponse.fromIntent(data)
        if (response == null) {
            repository.fail(if (AuthorizationException.fromIntent(data) != null) AuthFailure.AUTHORIZATION else AuthFailure.CANCELLED)
            return
        }
        service.performTokenRequest(response.createTokenExchangeRequest()) { tenantResponse, tokenError ->
            if (tenantResponse?.accessToken == null || tokenError != null) {
                repository.fail(AuthFailure.AUTHORIZATION)
                return@performTokenRequest
            }
            repository.startTokenExchange()
            exchangeEventToken(tenantResponse.accessToken.orEmpty()) { eventResponse, eventError ->
                if (eventResponse?.accessToken == null || eventError != null) {
                    repository.fail(AuthFailure.TOKEN_EXCHANGE)
                    return@exchangeEventToken
                }
                val subject = subjectFromJwt(tenantResponse.idToken ?: tenantResponse.accessToken.orEmpty())
                if (subject.isNullOrBlank()) {
                    repository.fail(AuthFailure.AUTHORIZATION)
                    return@exchangeEventToken
                }
                val now = System.currentTimeMillis() / 1000
                val tokens = AuthTokens(
                    tenantAccessToken = tenantResponse.accessToken.orEmpty(),
                    tenantExpiresAtEpochSeconds = tenantResponse.accessTokenExpirationTime?.div(1000) ?: now + 900,
                    refreshToken = tenantResponse.refreshToken,
                    idToken = tenantResponse.idToken,
                    eventAccessToken = eventResponse.accessToken.orEmpty(),
                    eventExpiresAtEpochSeconds = eventResponse.accessTokenExpirationTime?.div(1000) ?: now + 600,
                    subject = subject,
                )
                vault.save(tokens)
                repository.authenticate(tokens)
            }
        }
    }

    fun logout(launch: (Intent) -> Unit) {
        val idToken = repository.currentTokens()?.idToken
        vault.clear()
        repository.logout()
        val knownConfiguration = serviceConfiguration
        if (knownConfiguration != null) {
            launchEndSession(knownConfiguration, idToken, launch)
            return
        }
        AuthorizationServiceConfiguration.fetchFromIssuer(networkIssuer, { discovered, _ ->
            if (discovered != null) {
                launchEndSession(normalizeLocalOidcEndpointsForDebug(discovered, debugOriginOverride), idToken, launch)
            }
        }, connectionBuilder)
    }

    private fun launchEndSession(configuration: AuthorizationServiceConfiguration, idToken: String?, launch: (Intent) -> Unit) {
        if (configuration.endSessionEndpoint == null) return
        val builder = EndSessionRequest.Builder(configuration)
            .setPostLogoutRedirectUri(Uri.parse(config.oidcRedirectUri))
        if (!idToken.isNullOrBlank()) builder.setIdTokenHint(idToken)
        launch(service.getEndSessionRequestIntent(builder.build()))
    }

    private fun refresh(previous: AuthTokens) {
        val refreshToken = previous.refreshToken ?: run {
            vault.clear()
            repository.fail(AuthFailure.SESSION_EXPIRED)
            return
        }
        AuthorizationServiceConfiguration.fetchFromIssuer(networkIssuer, { discovered, error ->
            if (discovered == null || error != null) {
                repository.fail(AuthFailure.NETWORK)
                return@fetchFromIssuer
            }
            val configuration = normalizeLocalOidcEndpointsForDebug(discovered, debugOriginOverride)
            serviceConfiguration = configuration
            val request = TokenRequest.Builder(configuration, config.oidcClientId)
                .setGrantType("refresh_token")
                .setRefreshToken(refreshToken)
                .build()
            service.performTokenRequest(request) { tenantResponse, tokenError ->
                if (tenantResponse?.accessToken == null || tokenError != null) {
                    vault.clear()
                    repository.fail(AuthFailure.SESSION_EXPIRED)
                    return@performTokenRequest
                }
                exchangeEventToken(tenantResponse.accessToken.orEmpty()) { eventResponse, eventError ->
                    if (eventResponse?.accessToken == null || eventError != null) {
                        repository.fail(AuthFailure.TOKEN_EXCHANGE)
                        return@exchangeEventToken
                    }
                    val now = System.currentTimeMillis() / 1000
                    val refreshed = AuthTokens(
                        tenantAccessToken = tenantResponse.accessToken.orEmpty(),
                        tenantExpiresAtEpochSeconds = tenantResponse.accessTokenExpirationTime?.div(1000) ?: now + 900,
                        refreshToken = tenantResponse.refreshToken ?: refreshToken,
                        idToken = tenantResponse.idToken ?: previous.idToken,
                        eventAccessToken = eventResponse.accessToken.orEmpty(),
                        eventExpiresAtEpochSeconds = eventResponse.accessTokenExpirationTime?.div(1000) ?: now + 600,
                        subject = previous.subject,
                    )
                    vault.save(refreshed)
                    repository.authenticate(refreshed)
                }
            }
        }, connectionBuilder)
    }

    private fun exchangeEventToken(
        tenantAccessToken: String,
        callback: AuthorizationService.TokenResponseCallback,
    ) {
        val discovered = serviceConfiguration ?: run {
            repository.fail(AuthFailure.CONFIGURATION)
            return
        }
        val request = TokenRequest.Builder(discovered, config.oidcClientId)
            .setGrantType("urn:ietf:params:oauth:grant-type:token-exchange")
            .setScope(config.oidcScopes.split(' ').filter { it.startsWith("events.") }.joinToString(" "))
            .setAdditionalParameters(mapOf(
                "subject_token" to tenantAccessToken,
                "subject_token_type" to "urn:ietf:params:oauth:token-type:access_token",
                "audience" to config.oidcAudience,
                "resource" to config.eventResource(),
            ))
            .build()
        service.performTokenRequest(request, callback)
    }

    private fun subjectFromJwt(jwt: String): String? = runCatching {
        val payload = jwt.split('.')[1]
        JSONObject(String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP))).getString("sub")
    }.getOrNull()
}
