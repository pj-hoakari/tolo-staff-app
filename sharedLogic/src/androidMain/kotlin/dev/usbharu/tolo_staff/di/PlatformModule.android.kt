package dev.usbharu.tolo_staff.di

import dev.usbharu.tolo_staff.feature.appshell.AssignmentStatusService
import dev.usbharu.tolo_staff.feature.appshell.EventRepository
import dev.usbharu.tolo_staff.feature.appshell.GrpcAssignmentStatusService
import dev.usbharu.tolo_staff.feature.appshell.GrpcEventRepository
import dev.usbharu.tolo_staff.feature.appshell.GrpcReportRepository
import dev.usbharu.tolo_staff.feature.appshell.ReportRepository
import dev.usbharu.tolo_staff.feature.contactchat.ContactChatService
import dev.usbharu.tolo_staff.feature.contactchat.GrpcContactChatService
import dev.usbharu.tolo_staff.streaming.FirestoreOperationsStreamDataSource
import dev.usbharu.tolo_staff.streaming.AndroidFirebaseAuthGateway
import dev.usbharu.tolo_staff.streaming.AndroidInstallationIdProvider
import dev.usbharu.tolo_staff.streaming.FirebaseAuthGateway
import dev.usbharu.tolo_staff.streaming.FirebaseSessionBroker
import dev.usbharu.tolo_staff.streaming.FirebaseSessionController
import dev.usbharu.tolo_staff.streaming.GrpcFirebaseSessionBroker
import dev.usbharu.tolo_staff.streaming.InstallationIdProvider
import dev.usbharu.tolo_staff.streaming.GrpcCommunicationClient
import dev.usbharu.tolo_staff.streaming.OperationsChangeNotifier
import dev.usbharu.tolo_staff.streaming.FirestoreOperationsChangeNotifier
import dev.usbharu.tolo_staff.streaming.OperationsStreamDataSource
import dev.usbharu.tolo_staff.runtime.AppRuntimeConfig
import dev.usbharu.tolo_staff.runtime.AppSessionContext
import dev.usbharu.tolo_staff.runtime.GrpcMetadataProvider
import dev.usbharu.tolo_staff.streaming.OperationsStreamingConfig
import dev.usbharu.tolo_staff.feature.auth.AuthRepository
import dev.usbharu.tolo_staff.runtime.FirestoreAccessMode
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single {
        val config = get<AppRuntimeConfig>()
        OperationsStreamingConfig(
            enabled = config.firestoreEnabled && config.firestoreAccessMode != FirestoreAccessMode.DISABLED,
            host = config.firestoreHost,
            port = config.firestorePort,
            projectId = config.firestoreProjectId,
            databaseId = config.firestoreDatabaseId,
        )
    }
    single {
        val config = get<AppRuntimeConfig>()
        GrpcCommunicationClient(config.grpcHost, config.grpcPort, get<GrpcMetadataProvider>(), get<AuthRepository>())
    }
    single<FirebaseAuthGateway> { AndroidFirebaseAuthGateway() }
    single<InstallationIdProvider> { AndroidInstallationIdProvider() }
    single<FirebaseSessionBroker> {
        GrpcFirebaseSessionBroker(
            service = get<GrpcCommunicationClient>().firebaseSessionService,
            session = get(),
        )
    }
    single {
        FirebaseSessionController(
            config = get(),
            session = get(),
            authRepository = get(),
            broker = get(),
            authGateway = get(),
            installationIdProvider = get(),
        )
    }
    single<OperationsChangeNotifier> {
        FirestoreOperationsChangeNotifier(
            config = get(),
            session = get<AppSessionContext>(),
            firebaseSessionController = get<FirebaseSessionController>(),
        )
    }
    single<ReportRepository> { GrpcReportRepository(get(), get(), changeNotifier = get()) }
    single<EventRepository> { GrpcEventRepository(get(), get()) }
    single<AssignmentStatusService> { GrpcAssignmentStatusService(get(), get()) }
    single<OperationsStreamDataSource> {
        FirestoreOperationsStreamDataSource(
            grpcClient = get(),
            session = get(),
            changeNotifier = get(),
        )
    }
    single<ContactChatService> { GrpcContactChatService(get(), get(), get()) }
}
