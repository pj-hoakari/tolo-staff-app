package dev.usbharu.tolo_staff

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.usbharu.tolo_staff.di.KoinInitializer
import dev.usbharu.tolo_staff.runtime.AppRuntimeConfig
import dev.usbharu.tolo_staff.streaming.OperationsFirebaseBootstrap
import dev.usbharu.tolo_staff.streaming.OperationsStreamingConfig

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val runtimeConfig = AppRuntimeConfig(
            tenantId = BuildConfig.TOLO_TENANT_ID,
            eventId = BuildConfig.TOLO_EVENT_ID,
            staffId = BuildConfig.TOLO_STAFF_ID,
            grpcHost = BuildConfig.TOLO_GRPC_HOST,
            grpcPort = BuildConfig.TOLO_GRPC_PORT,
            firestoreEnabled = BuildConfig.TOLO_FIRESTORE_ENABLED,
            firestoreHost = BuildConfig.TOLO_FIRESTORE_HOST,
            firestorePort = BuildConfig.TOLO_FIRESTORE_PORT,
            firestoreProjectId = BuildConfig.TOLO_FIRESTORE_PROJECT_ID,
            firestoreDatabaseId = BuildConfig.TOLO_FIRESTORE_DATABASE_ID.ifBlank { null },
        )
        OperationsFirebaseBootstrap().initialize(
            applicationContext,
            OperationsStreamingConfig(
                enabled = runtimeConfig.firestoreEnabled,
                host = runtimeConfig.firestoreHost,
                port = runtimeConfig.firestorePort,
                projectId = runtimeConfig.firestoreProjectId,
                databaseId = runtimeConfig.firestoreDatabaseId,
            )
        )
        KoinInitializer.start(runtimeConfig)

        setContent {
            ToloStaffAndroidApp()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    ToloStaffAndroidApp()
}
