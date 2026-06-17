package dev.usbharu.tolo_staff

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.usbharu.tolo_staff.di.KoinInitializer
import dev.usbharu.tolo_staff.streaming.OperationsFirebaseBootstrap

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OperationsFirebaseBootstrap().initialize(applicationContext)
        KoinInitializer.start()

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
