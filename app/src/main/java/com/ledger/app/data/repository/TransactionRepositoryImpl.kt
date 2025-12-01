package com.ledger.app.data.repository

import com.ledger.app.data.local.dao.TransactionDao
import com.ledger.app.data.local.entity.TransactionEntity
import com.ledger.app.domain.model.Transaction
import com.ledger.app.domain.model.TransactionDirection
import com.ledger.app.domain.model.TransactionStatus
import com.ledger.app.domain.model.TransactionType
import com.ledger.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao
) : TransactionRepository {

    override suspend fun insert(transaction: Transaction): Long {
        return transactionDao.insert(transaction.toEntity())
    }

    override suspend fun update(transaction: Transaction) {
        transactionDao.update(transaction.toEntity())
    }

    override suspend fun softDelete(transactionId: Long) {
        transactionDao.softDelete(transactionId)
    }

    override suspend fun getById(id: Long): Transaction? {
        return transactionDao.getById(id)?.toDomain()
    }

    override fun getAll(): Flow<List<Transaction>> {
        return transactionDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getByCounterparty(counterpartyId: Long): Flow<List<Transaction>> {
        return transactionDao.getByCounterparty(counterpartyId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getByDateRange(start: LocalDateTime, end: LocalDateTime): Flow<List<Transaction>> {
        val startMillis = start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = end.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return transactionDao.getByDateRange(startMillis, endMillis).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getOutstandingObligations(): Flow<List<Transaction>> {
        return transactionDao.getOutstandingObligations().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> {
        return transactionDao.getRecentTransactions(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getTotalOwedToUser(): BigDecimal {
        return transactionDao.getTotalOwedToUser()
            ?.let { BigDecimal.valueOf(it) }
            ?: BigDecimal.ZERO
    }

    override suspend fun getTotalUserOwes(): BigDecimal {
        return transactionDao.getTotalUserOwes()
            ?.let { BigDecimal.valueOf(it) }
            ?: BigDecimal.ZERO
    }

    override fun getBillPayments(): Flow<List<Transaction>> {
        return transactionDao.getBillPayments().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun searchByNote(query: String): List<Transaction> {
        return transactionDao.searchByNote(query).map { it.toDomain() }
    }

    override fun getAllBillPaymentsAndRecharges(): Flow<List<Transaction>> {
        return transactionDao.getAllBillPaymentsAndRecharges().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getBillPaymentsByForSelf(isForSelf: Boolean): Flow<List<Transaction>> {
        return transactionDao.getBillPaymentsByForSelf(isForSelf).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getBillPaymentsByCategoryAndForSelf(category: String, isForSelf: Boolean): Flow<List<Transaction>> {
        return transactionDao.getBillPaymentsByCategoryAndForSelf(category, isForSelf).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    private fun Transaction.toEntity(): TransactionEntity {
        return TransactionEntity(
            id = id,
            direction = direction.name,
            type = type.name,
            amount = amount.toPlainString(),
            accountId = accountId,
            counterpartyId = counterpartyId,
            categoryId = categoryId,
            transactionDateTime = transactionDateTime.atZone(ZoneId.systemDefault())
                .toInstant().toEpochMilli(),
            status = status.name,
            notes = notes,
            remainingDue = remainingDue.toPlainString(),
            consumerId = consumerId,
            billCategory = billCategory?.name,
            isForSelf = isForSelf,
            linkedTaskId = linkedTaskId
        )
    }

    private fun TransactionEntity.toDomain(): Transaction {
        return Transaction(
            id = id,
            direction = TransactionDirection.valueOf(direction),
            type = TransactionType.valueOf(type),
            amount = BigDecimal(amount),
            accountId = accountId,
            counterpartyId = counterpartyId,
            categoryId = categoryId,
            transactionDateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(transactionDateTime),
                ZoneId.systemDefault()
            ),
            status = TransactionStatus.valueOf(status),
            notes = notes,
            remainingDue = BigDecimal(remainingDue),
            consumerId = consumerId,
            billCategory = billCategory?.let { 
                try { com.ledger.app.domain.model.BillCategory.valueOf(it) } catch (e: Exception) { null }
            },
            isForSelf = isForSelf,
            linkedTaskId = linkedTaskId
        )
    }
}
