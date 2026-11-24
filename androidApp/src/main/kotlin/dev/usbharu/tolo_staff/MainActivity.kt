package dev.usbharu.tolo_staff

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.usbharu.tolo_staff.di.KoinInitializer
import dev.usbharu.tolo_staff.streaming.OperationsFirebaseBootstrap
import dev.usbharu.tolo_staff.streaming.OperationsScope

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OperationsFirebaseBootstrap().initialize(applicationContext)
        KoinInitializer.start(
            OperationsScope(
                eventId = BuildConfig.TOLO_EVENT_ID,
                tenantId = BuildConfig.TOLO_TENANT_ID,
                accessToken = BuildConfig.TOLO_ACCESS_TOKEN,
            )
        )

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
