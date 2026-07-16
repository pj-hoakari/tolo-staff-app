package dev.usbharu.tolo_staff

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import dev.usbharu.tolo_staff.di.KoinInitializer
import dev.usbharu.tolo_staff.runtime.AppRuntimeConfig
import dev.usbharu.tolo_staff.runtime.FirestoreAccessMode
import dev.usbharu.tolo_staff.streaming.OperationsFirebaseBootstrap

class MainActivity : ComponentActivity() {
    private var processLifecycleObserver: DefaultLifecycleObserver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val runtimeConfig = AppRuntimeConfig(
            tenantId = BuildConfig.TOLO_TENANT_ID,
            eventId = BuildConfig.TOLO_EVENT_ID,
            staffId = if (BuildConfig.TOLO_OIDC_ENABLED) "unauthenticated" else BuildConfig.TOLO_STAFF_ID,
            grpcHost = BuildConfig.TOLO_GRPC_HOST,
            grpcPort = BuildConfig.TOLO_GRPC_PORT,
            firestoreEnabled = BuildConfig.TOLO_FIRESTORE_ENABLED,
            firestoreHost = BuildConfig.TOLO_FIRESTORE_HOST,
            firestorePort = BuildConfig.TOLO_FIRESTORE_PORT,
            firestoreProjectId = BuildConfig.TOLO_FIRESTORE_PROJECT_ID,
            firestoreDatabaseId = BuildConfig.TOLO_FIRESTORE_DATABASE_ID.ifBlank { null },
            firestoreAccessMode = when {
                !BuildConfig.TOLO_FIRESTORE_ENABLED -> FirestoreAccessMode.DISABLED
                BuildConfig.DEBUG && !BuildConfig.TOLO_OIDC_ENABLED ->
                    FirestoreAccessMode.DEBUG_EMULATOR_ANONYMOUS
                else -> FirestoreAccessMode.AUTHENTICATED
            },
            firestoreEmulatorEnabled = BuildConfig.TOLO_FIRESTORE_EMULATOR_ENABLED,
            firebaseAuthEmulatorEnabled = BuildConfig.TOLO_FIREBASE_AUTH_EMULATOR_ENABLED,
            firebaseAuthHost = BuildConfig.TOLO_FIREBASE_AUTH_HOST,
            firebaseAuthPort = BuildConfig.TOLO_FIREBASE_AUTH_PORT,
            firebaseApplicationId = BuildConfig.TOLO_FIREBASE_APPLICATION_ID,
            firebaseApiKey = BuildConfig.TOLO_FIREBASE_API_KEY,
            firebaseGcmSenderId = BuildConfig.TOLO_FIREBASE_GCM_SENDER_ID,
            isDebugBuild = BuildConfig.DEBUG,
            oidcLoginEnabled = BuildConfig.TOLO_OIDC_ENABLED,
            oidcIssuer = BuildConfig.TOLO_OIDC_ISSUER,
            oidcClientId = BuildConfig.TOLO_OIDC_CLIENT_ID,
            oidcRedirectUri = BuildConfig.TOLO_OIDC_REDIRECT_URI,
            oidcScopes = BuildConfig.TOLO_OIDC_SCOPES,
            oidcAudience = BuildConfig.TOLO_OIDC_AUDIENCE,
            oidcResourceBaseUrl = BuildConfig.TOLO_OIDC_RESOURCE_BASE_URL,
        )
        OperationsFirebaseBootstrap().initialize(applicationContext, runtimeConfig.requireValid())
        KoinInitializer.start(runtimeConfig)
        val koinHelper = dev.usbharu.tolo_staff.di.KoinHelper()
        val firebaseSessionController = koinHelper.getFirebaseSessionController()
        processLifecycleObserver = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                firebaseSessionController.onForeground()
            }

            override fun onStop(owner: LifecycleOwner) {
                firebaseSessionController.onBackground()
            }
        }.also(ProcessLifecycleOwner.get().lifecycle::addObserver)
        val oidcCoordinator = if (runtimeConfig.oidcLoginEnabled) {
            AndroidOidcCoordinator(
                context = this,
                config = runtimeConfig,
                repository = koinHelper.getAuthRepository(),
                firebaseSessionController = firebaseSessionController,
            )
        } else {
            null
        }

        setContent {
            if (oidcCoordinator == null) {
                ToloStaffAndroidApp()
            } else {
                AndroidAuthRoot(oidcCoordinator)
            }
        }
    }

    override fun onDestroy() {
        processLifecycleObserver?.let(ProcessLifecycleOwner.get().lifecycle::removeObserver)
        processLifecycleObserver = null
        super.onDestroy()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    ToloStaffAndroidApp()
}
