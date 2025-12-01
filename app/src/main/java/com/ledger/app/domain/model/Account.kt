package com.ledger.app.domain.model

/**
 * Domain model representing a wallet or payment method.
 */
data class Account(
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val isActive: Boolean = true
)
