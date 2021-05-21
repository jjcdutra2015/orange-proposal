package br.com.zup.edu.shared.grpc

import br.com.zup.edu.CreateProposalEndpoint
import br.com.zup.edu.ProposalAlreadyExistsException
import com.google.rpc.BadRequest
import com.google.rpc.Code
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import io.micronaut.aop.InterceptorBean
import io.micronaut.aop.MethodInterceptor
import io.micronaut.aop.MethodInvocationContext
import org.slf4j.LoggerFactory
import javax.inject.Singleton
import javax.validation.ConstraintViolationException

@Singleton
@InterceptorBean(ErrorHandler::class)
class ExceptionHandlerInterceptor : MethodInterceptor<CreateProposalEndpoint, Any?> {

    private val LOGGER = LoggerFactory.getLogger(CreateProposalEndpoint::class.java)

    override fun intercept(context: MethodInvocationContext<CreateProposalEndpoint, Any?>): Any? {
        // antes
        LOGGER.info("Intercepting method ${context.targetMethod}")

        try {
            return context.proceed()
        } catch (e: Exception) {
            val error = when (e) {
                is IllegalArgumentException -> Status.INVALID_ARGUMENT.withDescription(e.message).asRuntimeException()
                is IllegalStateException -> Status.FAILED_PRECONDITION.withDescription(e.message).asRuntimeException()
                is ProposalAlreadyExistsException -> Status.ALREADY_EXISTS.withDescription(e.message)
                    .asRuntimeException()
                is ConstraintViolationException -> handleConstraintVaiolationException(e)
                else -> Status.UNKNOWN.withDescription("unexpected error happened").asRuntimeException()
            }

            val responseObserver = context.parameterValues[1] as StreamObserver<*>
            responseObserver.onError(error)

            return null
        }
    }

    private fun handleConstraintVaiolationException(e: ConstraintViolationException): StatusRuntimeException {
        val violations = e.constraintViolations.map {
            BadRequest.FieldViolation.newBuilder()
                .setField(it.propertyPath.last().name)
                .setDescription(it.message)
                .build()
        }

        val details = BadRequest.newBuilder()
            .addAllFieldViolations(violations).build()

        val statusProto = com.google.rpc.Status.newBuilder()
            .setCode(Code.INVALID_ARGUMENT_VALUE)
            .setMessage("Invalid parameters")
            .addDetails(com.google.protobuf.Any.pack(details))
            .build()

        LOGGER.info("Status proto: $statusProto")
        return io.grpc.protobuf.StatusProto.toStatusRuntimeException(statusProto)
    }
}