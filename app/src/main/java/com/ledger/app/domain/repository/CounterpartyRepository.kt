package com.ledger.app.domain.repository

import com.ledger.app.domain.model.Counterparty
import com.ledger.app.domain.model.CounterpartyWithBalance
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for counterparty data access.
 */
interface CounterpartyRepository {
    suspend fun insert(counterparty: Counterparty): Long
    suspend fun update(counterparty: Counterparty)
    suspend fun delete(counterpartyId: Long)
    suspend fun getById(id: Long): Counterparty?
    fun getAll(): Flow<List<Counterparty>>
    fun search(query: String): Flow<List<Counterparty>>
    fun getAllWithBalances(): Flow<List<CounterpartyWithBalance>>
}
