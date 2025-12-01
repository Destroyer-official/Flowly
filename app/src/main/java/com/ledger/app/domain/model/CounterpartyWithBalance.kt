package com.ledger.app.domain.model

import java.math.BigDecimal

/**
 * Domain model representing a counterparty with their calculated net balance.
 * 
 * @property counterparty The counterparty details
 * @property netBalance The net balance: positive means they owe user, negative means user owes them
 */
data class CounterpartyWithBalance(
    val counterparty: Counterparty,
    val netBalance: BigDecimal
)
