package br.com.zup.edu

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.PositiveOrZero

@Entity
class Proposal(
    @field:NotBlank
    @Column(nullable = false)
    val name: String,

    @CpfOrCnpj
    @field:NotBlank
    @Column(nullable = false, unique = true)
    val document: String,

    @field:Email
    @field:NotBlank
    @Column(nullable = false)
    val email: String,

    @field:NotBlank
    @Column(nullable = false)
    val address: String,

    @field:PositiveOrZero
    @field:NotNull
    @Column(nullable = false)
    val salary: BigDecimal
) {
    @Id
    @GeneratedValue
    val id: UUID? = null

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
}
