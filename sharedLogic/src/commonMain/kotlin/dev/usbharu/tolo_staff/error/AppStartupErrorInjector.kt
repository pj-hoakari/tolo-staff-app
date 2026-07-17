package dev.usbharu.tolo_staff.error

import kotlinx.coroutines.flow.MutableStateFlow

enum class AppStartupErrorScenario {
    NONE,
    GRPC_CONNECTION,
    FATAL,
}

interface AppStartupErrorInjector {
    fun inject(errorReporter: AppErrorReporter)
}

object NoOpAppStartupErrorInjector : AppStartupErrorInjector {
    override fun inject(errorReporter: AppErrorReporter) = Unit
}

class ScenarioAppStartupErrorInjector(
    private val scenario: AppStartupErrorScenario,
) : AppStartupErrorInjector {
    private val injected = MutableStateFlow(false)

    override fun inject(errorReporter: AppErrorReporter) {
        if (!injected.compareAndSet(expect = false, update = true)) return

        when (scenario) {
            AppStartupErrorScenario.NONE -> Unit
            AppStartupErrorScenario.GRPC_CONNECTION -> errorReporter.reportConnectionFailure(
                source = AppErrorSource.GRPC,
                operation = "Start the UI test connection scenario",
                connectionKey = "ui-test.startup.grpc",
            )
            AppStartupErrorScenario.FATAL -> errorReporter.reportFatal(
                throwable = IllegalStateException("UI test fatal error scenario"),
                operation = "Start the UI test fatal scenario",
                failureKey = "ui-test.startup.fatal",
            )
        }
    }
}
