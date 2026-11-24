package dev.usbharu.tolo_staff.di

import org.koin.core.module.Module

actual fun platformModule(): Module = grpcOperationsModule()
