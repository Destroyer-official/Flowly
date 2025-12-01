package com.ledger.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ledger.app.data.local.entity.PartialPaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PartialPaymentDao {
    @Insert
    suspend fun insert(payment: PartialPaymentEntity): Long

    @Query("DELETE FROM partial_payments WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM partial_payments WHERE parentTransactionId = :transactionId ORDER BY dateTime")
    fun getByTransaction(transactionId: Long): Flow<List<PartialPaymentEntity>>

    @Query("SELECT SUM(CAST(amount AS REAL)) FROM partial_payments WHERE parentTransactionId = :transactionId")
    suspend fun getTotalPaidForTransaction(transactionId: Long): Double?

    // Backup/Restore operations
    @Query("SELECT * FROM partial_payments")
    suspend fun getAllForBackup(): List<PartialPaymentEntity>

    @Query("DELETE FROM partial_payments")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(payments: List<PartialPaymentEntity>)
}
