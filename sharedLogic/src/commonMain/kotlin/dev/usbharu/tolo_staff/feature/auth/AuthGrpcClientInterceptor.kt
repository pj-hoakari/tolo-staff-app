package dev.usbharu.tolo_staff.feature.auth

import kotlinx.coroutines.flow.Flow
import kotlinx.rpc.grpc.GrpcStatusCode
import kotlinx.rpc.grpc.client.GrpcClientCallScope
import kotlinx.rpc.grpc.client.GrpcClientInterceptor
import kotlinx.rpc.grpc.statusCode

class AuthGrpcClientInterceptor(
    private val repository: AuthRepository,
) : GrpcClientInterceptor {
    override fun <Request, Response> GrpcClientCallScope<Request, Response>.intercept(
        request: Flow<Request>,
    ): Flow<Response> {
        onClose { status, _ ->
            if (status.statusCode == GrpcStatusCode.UNAUTHENTICATED) {
                repository.fail(AuthFailure.SESSION_EXPIRED)
            }
        }
        return proceed(request)
    }
}
