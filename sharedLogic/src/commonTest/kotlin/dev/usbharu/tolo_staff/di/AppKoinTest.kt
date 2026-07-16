package dev.usbharu.tolo_staff.di

import dev.usbharu.tolo_staff.feature.auth.AuthGrpcMetadataProvider
import dev.usbharu.tolo_staff.runtime.AppRuntimeConfig
import dev.usbharu.tolo_staff.runtime.GrpcMetadataProvider
import dev.usbharu.tolo_staff.runtime.NoOpGrpcMetadataProvider
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertSame
import org.koin.dsl.koinApplication

class AppKoinTest {
    @Test
    fun `disabled oidc selects no op grpc metadata`() {
        val suppliedMetadata = GrpcMetadataProvider { mapOf("authorization" to "Bearer must-not-be-used") }
        val application = koinApplication {
            modules(coreModule(config(oidcLoginEnabled = false), suppliedMetadata))
        }
        try {
            assertSame(NoOpGrpcMetadataProvider, application.koin.get<GrpcMetadataProvider>())
        } finally {
            application.close()
        }
    }

    @Test
    fun `enabled oidc selects bearer grpc metadata`() {
        val application = koinApplication {
            modules(coreModule(config(oidcLoginEnabled = true), NoOpGrpcMetadataProvider))
        }
        try {
            assertIs<AuthGrpcMetadataProvider>(application.koin.get<GrpcMetadataProvider>())
        } finally {
            application.close()
        }
    }

    private fun config(oidcLoginEnabled: Boolean) = AppRuntimeConfig(
        tenantId = "tenant",
        eventId = "event",
        staffId = "staff",
        grpcHost = "localhost",
        oidcLoginEnabled = oidcLoginEnabled,
    )
}
