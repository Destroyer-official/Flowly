package com.ledger.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ledger.app.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("UPDATE transactions SET isSoftDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)

    @Query("SELECT * FROM transactions WHERE id = :id AND isSoftDeleted = 0")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE isSoftDeleted = 0 ORDER BY transactionDateTime DESC")
    fun getAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE counterpartyId = :counterpartyId AND isSoftDeleted = 0 ORDER BY transactionDateTime DESC")
    fun getByCounterparty(counterpartyId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE status IN ('PENDING', 'PARTIALLY_SETTLED') AND isSoftDeleted = 0")
    fun getOutstandingObligations(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isSoftDeleted = 0 ORDER BY transactionDateTime DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int): Flow<List<TransactionEntity>>

    @Query("""
        SELECT SUM(CAST(remainingDue AS REAL)) FROM transactions
        WHERE direction = 'GAVE' AND status != 'CANCELLED' AND isSoftDeleted = 0
    """)
    suspend fun getTotalOwedToUser(): Double?

    @Query("""
        SELECT SUM(CAST(remainingDue AS REAL)) FROM transactions
        WHERE direction = 'RECEIVED' AND status != 'CANCELLED' AND isSoftDeleted = 0
    """)
    suspend fun getTotalUserOwes(): Double?

    @Query("""
        SELECT * FROM transactions 
        WHERE transactionDateTime >= :startMillis 
        AND transactionDateTime < :endMillis 
        AND isSoftDeleted = 0 
        ORDER BY transactionDateTime DESC
    """)
    fun getByDateRange(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE type = 'BILL_PAYMENT' AND isSoftDeleted = 0 
        ORDER BY transactionDateTime DESC
    """)
    fun getBillPayments(): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE billCategory = :category AND isSoftDeleted = 0 
        ORDER BY transactionDateTime DESC
    """)
    fun getBillPaymentsByCategory(category: String): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE (notes LIKE '%' || :query || '%' OR consumerId LIKE '%' || :query || '%')
        AND isSoftDeleted = 0 
        ORDER BY transactionDateTime DESC
    """)
    suspend fun searchByNote(query: String): List<TransactionEntity>

    @Query("""
        SELECT SUM(CAST(amount AS REAL)) FROM transactions
        WHERE type = 'BILL_PAYMENT' 
        AND billCategory = :category
        AND transactionDateTime >= :startMillis 
        AND transactionDateTime < :endMillis 
        AND isSoftDeleted = 0
    """)
    suspend fun getBillTotalByCategory(category: String, startMillis: Long, endMillis: Long): Double?

    @Query("""
        SELECT * FROM transactions 
        WHERE type IN ('BILL_PAYMENT', 'RECHARGE') 
        AND isForSelf = :isForSelf
        AND isSoftDeleted = 0 
        ORDER BY transactionDateTime DESC
    """)
    fun getBillPaymentsByForSelf(isForSelf: Boolean): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE type IN ('BILL_PAYMENT', 'RECHARGE') 
        AND billCategory = :category
        AND isForSelf = :isForSelf
        AND isSoftDeleted = 0 
        ORDER BY transactionDateTime DESC
    """)
    fun getBillPaymentsByCategoryAndForSelf(category: String, isForSelf: Boolean): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE type IN ('BILL_PAYMENT', 'RECHARGE') 
        AND isSoftDeleted = 0 
        ORDER BY transactionDateTime DESC
    """)
    fun getAllBillPaymentsAndRecharges(): Flow<List<TransactionEntity>>

    // Task linking queries (Requirements: 15.5)
    @Query("SELECT * FROM transactions WHERE linkedTaskId = :taskId AND isSoftDeleted = 0")
    suspend fun getByLinkedTask(taskId: Long): TransactionEntity?

    // Backup/Restore operations
    @Query("SELECT * FROM transactions")
    suspend fun getAllForBackup(): List<TransactionEntity>

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(transactions: List<TransactionEntity>)
}
