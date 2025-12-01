package com.ledger.app.data.repository

import com.ledger.app.data.local.dao.CounterpartyDao
import com.ledger.app.data.local.dao.TransactionDao
import com.ledger.app.data.local.entity.CounterpartyEntity
import com.ledger.app.domain.model.Counterparty
import com.ledger.app.domain.model.CounterpartyWithBalance
import com.ledger.app.domain.repository.CounterpartyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import javax.inject.Inject

class CounterpartyRepositoryImpl @Inject constructor(
    private val counterpartyDao: CounterpartyDao,
    private val transactionDao: TransactionDao
) : CounterpartyRepository {

    override suspend fun insert(counterparty: Counterparty): Long {
        return counterpartyDao.insert(counterparty.toEntity())
    }

    override suspend fun update(counterparty: Counterparty) {
        counterpartyDao.update(counterparty.toEntity())
    }

    override suspend fun delete(counterpartyId: Long) {
        counterpartyDao.delete(counterpartyId)
    }

    override suspend fun getById(id: Long): Counterparty? {
        return counterpartyDao.getById(id)?.toDomain()
    }

    override fun getAll(): Flow<List<Counterparty>> {
        return counterpartyDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun search(query: String): Flow<List<Counterparty>> {
        return counterpartyDao.search(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllWithBalances(): Flow<List<CounterpartyWithBalance>> {
        return combine(
            counterpartyDao.getAll(),
            transactionDao.getAll()
        ) { counterparties, transactions ->
            counterparties.map { counterparty ->
                val counterpartyTransactions = transactions.filter { 
                    it.counterpartyId == counterparty.id && it.status != "CANCELLED"
                }
                
                // Calculate net balance: positive = they owe user, negative = user owes them
                val gaveTotal = counterpartyTransactions
                    .filter { it.direction == "GAVE" }
                    .sumOf { BigDecimal(it.remainingDue) }
                
                val receivedTotal = counterpartyTransactions
                    .filter { it.direction == "RECEIVED" }
                    .sumOf { BigDecimal(it.remainingDue) }
                
                val netBalance = gaveTotal - receivedTotal
                
                CounterpartyWithBalance(
                    counterparty = counterparty.toDomain(),
                    netBalance = netBalance
                )
            }
        }
    }

    private fun Counterparty.toEntity(): CounterpartyEntity {
        return CounterpartyEntity(
            id = id,
            displayName = displayName,
            phoneNumber = phoneNumber,
            notes = notes,
            isFavorite = isFavorite
        )
    }

    private fun CounterpartyEntity.toDomain(): Counterparty {
        return Counterparty(
            id = id,
            displayName = displayName,
            phoneNumber = phoneNumber,
            notes = notes,
            isFavorite = isFavorite
        )
    }
}
