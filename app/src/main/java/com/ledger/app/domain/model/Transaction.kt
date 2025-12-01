package com.ledger.app.domain.model

import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Domain model representing a financial transaction.
 */
data class Transaction(
    val id: Long = 0,
    val direction: TransactionDirection,
    val type: TransactionType,
    val amount: BigDecimal,
    val accountId: Long,
    val counterpartyId: Long? = null,
    val categoryId: Long,
    val transactionDateTime: LocalDateTime,
    val status: TransactionStatus = TransactionStatus.PENDING,
    val notes: String? = null,
    val remainingDue: BigDecimal,
    val consumerId: String? = null,      // Phone number or consumer ID for bill payments
    val billCategory: BillCategory? = null, // Category for bill payments (Electricity, TV, etc.)
    val isForSelf: Boolean = false,      // True if bill paid for self (no debt), false if for others (adds to debt)
    val linkedTaskId: Long? = null       // ID of the task this transaction was created from (Requirements: 15.5)
)
