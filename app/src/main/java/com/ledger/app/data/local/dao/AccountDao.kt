package com.ledger.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ledger.app.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Insert
    suspend fun insert(account: AccountEntity): Long

    @Update
    suspend fun update(account: AccountEntity)

    @Query("UPDATE accounts SET isActive = 0 WHERE id = :id")
    suspend fun deactivate(id: Long)

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE isActive = 1 ORDER BY name")
    fun getActive(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY name")
    fun getAll(): Flow<List<AccountEntity>>
    
    @Query("SELECT * FROM accounts")
    suspend fun getAllAccounts(): List<AccountEntity>

    // Backup/Restore operations
    @Query("SELECT * FROM accounts")
    suspend fun getAllForBackup(): List<AccountEntity>

    @Query("DELETE FROM accounts")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(accounts: List<AccountEntity>)
}
