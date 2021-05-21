package br.com.zup.edu

import com.google.protobuf.Any
import com.google.protobuf.Timestamp
import com.google.rpc.BadRequest
import com.google.rpc.Code
import com.google.rpc.StatusProto
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.ConstraintViolationException

@Singleton
open class CreateProposalEndpoint(val repository: ProposalRepository) :
    PropostaGrpcServiceGrpc.PropostaGrpcServiceImplBase() {

    private val LOGGER = LoggerFactory.getLogger(CreateProposalEndpoint::class.java)

    @Transactional
    open override fun create(request: CreateProposalRequest, responseObserver: StreamObserver<CreateProposalResponse>) {

        if (repository.existsByDocument(request.document)) {
            responseObserver.onError(
                Status.ALREADY_EXISTS.withDescription("Document already exists").asRuntimeException()
            )
        }

        LOGGER.info("new request: $request")


        val proposal = try {
            repository.save(request.toModel())
        } catch (e: ConstraintViolationException) {
            LOGGER.error("Invalid error: ${e.message}")
            responseObserver.onError(handleConstraintVaiolationException(e))
            return
        }

        val response = CreateProposalResponse.newBuilder()
            .setId(proposal.id.toString())
            .setCreatedAt(proposal.createdAt.toGrpcTimestamp()).build()

        responseObserver.onNext(response)
        responseObserver.onCompleted()
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
            .addDetails(Any.pack(details))
            .build()

        LOGGER.info("Status proto: $statusProto")
        val error = io.grpc.protobuf.StatusProto.toStatusRuntimeException(statusProto)
        return error
    }
}

fun CreateProposalRequest.toModel(): Proposal {
    return Proposal(
        name = this.name,
        document = this.document,
        email = this.email,
        address = this.address,
        salary = BigDecimal(this.salary)
    )
}

fun LocalDateTime.toGrpcTimestamp(): Timestamp {
    val instant = this.atZone(ZoneId.of("UTC")).toInstant()
    return Timestamp.newBuilder()
        .setSeconds(instant.epochSecond)
        .setNanos(instant.nano)
        .build()
}