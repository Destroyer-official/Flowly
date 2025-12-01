package com.ledger.app.domain.model

import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Domain model representing a partial payment against a transaction.
 */
data class PartialPayment(
    val id: Long = 0,
    val parentTransactionId: Long,
    val amount: BigDecimal,
    val direction: PaymentDirection,
    val dateTime: LocalDateTime,
    val method: PaymentMethod,
    val notes: String? = null
)
