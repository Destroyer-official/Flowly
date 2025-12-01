package com.ledger.app.data.repository

import com.ledger.app.data.local.dao.PartialPaymentDao
import com.ledger.app.data.local.entity.PartialPaymentEntity
import com.ledger.app.domain.model.PartialPayment
import com.ledger.app.domain.model.PaymentDirection
import com.ledger.app.domain.model.PaymentMethod
import com.ledger.app.domain.repository.PartialPaymentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

class PartialPaymentRepositoryImpl @Inject constructor(
    private val partialPaymentDao: PartialPaymentDao
) : PartialPaymentRepository {

    override suspend fun insert(payment: PartialPayment): Long {
        return partialPaymentDao.insert(payment.toEntity())
    }

    override suspend fun delete(paymentId: Long) {
        partialPaymentDao.delete(paymentId)
    }

    override fun getByTransaction(transactionId: Long): Flow<List<PartialPayment>> {
        return partialPaymentDao.getByTransaction(transactionId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getTotalPaidForTransaction(transactionId: Long): BigDecimal {
        return partialPaymentDao.getTotalPaidForTransaction(transactionId)
            ?.let { BigDecimal.valueOf(it) }
            ?: BigDecimal.ZERO
    }

    private fun PartialPayment.toEntity(): PartialPaymentEntity {
        return PartialPaymentEntity(
            id = id,
            parentTransactionId = parentTransactionId,
            amount = amount.toPlainString(),
            direction = direction.name,
            dateTime = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            method = method.name,
            notes = notes
        )
    }

    private fun PartialPaymentEntity.toDomain(): PartialPayment {
        return PartialPayment(
            id = id,
            parentTransactionId = parentTransactionId,
            amount = BigDecimal(amount),
            direction = PaymentDirection.valueOf(direction),
            dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(dateTime),
                ZoneId.systemDefault()
            ),
            method = PaymentMethod.valueOf(method),
            notes = notes
        )
    }
}
