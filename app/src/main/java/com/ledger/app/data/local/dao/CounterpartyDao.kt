package com.ledger.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ledger.app.data.local.entity.CounterpartyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CounterpartyDao {
    @Insert
    suspend fun insert(counterparty: CounterpartyEntity): Long

    @Update
    suspend fun update(counterparty: CounterpartyEntity)

    @Query("DELETE FROM counterparties WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM counterparties WHERE id = :id")
    suspend fun getById(id: Long): CounterpartyEntity?

    @Query("SELECT * FROM counterparties ORDER BY isFavorite DESC, displayName ASC")
    fun getAll(): Flow<List<CounterpartyEntity>>

    @Query("SELECT * FROM counterparties WHERE displayName LIKE '%' || :query || '%' OR phoneNumber LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<CounterpartyEntity>>

    // Backup/Restore operations
    @Query("SELECT * FROM counterparties")
    suspend fun getAllForBackup(): List<CounterpartyEntity>

    @Query("DELETE FROM counterparties")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(counterparties: List<CounterpartyEntity>)
}
