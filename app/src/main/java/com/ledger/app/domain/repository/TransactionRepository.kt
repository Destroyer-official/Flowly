package com.ledger.app.domain.repository

import com.ledger.app.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Repository interface for transaction data access.
 */
interface TransactionRepository {
    suspend fun insert(transaction: Transaction): Long
    suspend fun update(transaction: Transaction)
    suspend fun softDelete(transactionId: Long)
    suspend fun getById(id: Long): Transaction?
    fun getAll(): Flow<List<Transaction>>
    fun getByCounterparty(counterpartyId: Long): Flow<List<Transaction>>
    fun getByDateRange(start: LocalDateTime, end: LocalDateTime): Flow<List<Transaction>>
    fun getOutstandingObligations(): Flow<List<Transaction>>
    fun getRecentTransactions(limit: Int): Flow<List<Transaction>>
    suspend fun getTotalOwedToUser(): BigDecimal
    suspend fun getTotalUserOwes(): BigDecimal
    fun getBillPayments(): Flow<List<Transaction>>
    suspend fun searchByNote(query: String): List<Transaction>
    
    // Bill tracking methods
    fun getAllBillPaymentsAndRecharges(): Flow<List<Transaction>>
    fun getBillPaymentsByForSelf(isForSelf: Boolean): Flow<List<Transaction>>
    fun getBillPaymentsByCategoryAndForSelf(category: String, isForSelf: Boolean): Flow<List<Transaction>>
}
