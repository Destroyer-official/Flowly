package com.ledger.app.domain.usecase

import com.ledger.app.domain.model.Counterparty
import com.ledger.app.domain.model.Transaction
import com.ledger.app.domain.model.TransactionDirection
import com.ledger.app.domain.model.TransactionStatus
import com.ledger.app.domain.repository.CounterpartyRepository
import com.ledger.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Use case for getting a counterparty's ledger.
 * 
 * Retrieves:
 * - Counterparty details
 * - Net balance (positive = they owe user, negative = user owes them)
 * - All transactions with that counterparty
 * 
 * Requirements: 4.1, 4.2
 */
class GetCounterpartyLedgerUseCase @Inject constructor(
    private val counterpartyRepository: CounterpartyRepository,
    private val transactionRepository: TransactionRepository
) {
    /**
     * Result of getting a counterparty ledger.
     */
    data class CounterpartyLedger(
        val counterparty: Counterparty,
        val netBalance: BigDecimal,
        val transactions: List<Transaction>
    )

    /**
     * Gets the counterparty ledger data.
     * 
     * @param counterpartyId The ID of the counterparty
     * @return CounterpartyLedger or null if counterparty not found
     */
    suspend operator fun invoke(counterpartyId: Long): CounterpartyLedger? {
        val counterparty = counterpartyRepository.getById(counterpartyId)
            ?: return null

        val transactions = transactionRepository
            .getByCounterparty(counterpartyId)
            .first()

        val netBalance = calculateNetBalance(transactions)

        return CounterpartyLedger(
            counterparty = counterparty,
            netBalance = netBalance,
            transactions = transactions
        )
    }

    /**
     * Gets the counterparty ledger as a Flow for reactive updates.
     * 
     * @param counterpartyId The ID of the counterparty
     * @return Flow of CounterpartyLedger or null if counterparty not found
     */
    fun observeLedger(counterpartyId: Long): Flow<CounterpartyLedger?> {
        return transactionRepository.getByCounterparty(counterpartyId).map { transactions ->
            val counterparty = counterpartyRepository.getById(counterpartyId)
                ?: return@map null

            val netBalance = calculateNetBalance(transactions)

            CounterpartyLedger(
                counterparty = counterparty,
                netBalance = netBalance,
                transactions = transactions
            )
        }
    }

    /**
     * Calculates the net balance for a counterparty.
     * 
     * Property 4: Net Balance Calculation
     * Net balance = sum(GAVE transactions remaining_due) - sum(RECEIVED transactions remaining_due)
     * Positive means they owe user, negative means user owes them.
     * 
     * Cancelled transactions are excluded from the calculation.
     */
    internal fun calculateNetBalance(transactions: List<Transaction>): BigDecimal {
        val activeTransactions = transactions.filter { it.status != TransactionStatus.CANCELLED }
        
        val totalGave = activeTransactions
            .filter { it.direction == TransactionDirection.GAVE }
            .fold(BigDecimal.ZERO) { acc, t -> acc + t.remainingDue }

        val totalReceived = activeTransactions
            .filter { it.direction == TransactionDirection.RECEIVED }
            .fold(BigDecimal.ZERO) { acc, t -> acc + t.remainingDue }

        return totalGave - totalReceived
    }

    companion object {
        /**
         * Static helper for calculating net balance from a list of transactions.
         * Useful for testing.
         */
        fun calculateNetBalanceStatic(transactions: List<Transaction>): BigDecimal {
            val activeTransactions = transactions.filter { it.status != TransactionStatus.CANCELLED }
            
            val totalGave = activeTransactions
                .filter { it.direction == TransactionDirection.GAVE }
                .fold(BigDecimal.ZERO) { acc, t -> acc + t.remainingDue }

            val totalReceived = activeTransactions
                .filter { it.direction == TransactionDirection.RECEIVED }
                .fold(BigDecimal.ZERO) { acc, t -> acc + t.remainingDue }

            return totalGave - totalReceived
        }
    }
}
