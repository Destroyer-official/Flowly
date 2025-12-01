package com.ledger.app.domain.repository

import com.ledger.app.domain.model.PartialPayment
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

/**
 * Repository interface for partial payment data access.
 */
interface PartialPaymentRepository {
    suspend fun insert(payment: PartialPayment): Long
    suspend fun delete(paymentId: Long)
    fun getByTransaction(transactionId: Long): Flow<List<PartialPayment>>
    suspend fun getTotalPaidForTransaction(transactionId: Long): BigDecimal
}
