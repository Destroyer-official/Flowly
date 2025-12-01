package com.ledger.app.domain.repository

import com.ledger.app.domain.model.Account
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for account/wallet data access.
 */
interface AccountRepository {
    suspend fun insert(account: Account): Long
    suspend fun update(account: Account)
    suspend fun deactivate(accountId: Long)
    fun getActive(): Flow<List<Account>>
    suspend fun getById(id: Long): Account?
}
