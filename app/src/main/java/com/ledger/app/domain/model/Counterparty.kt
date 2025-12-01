package com.ledger.app.domain.model

/**
 * Domain model representing a person with whom the user has financial dealings.
 */
data class Counterparty(
    val id: Long = 0,
    val displayName: String,
    val phoneNumber: String? = null,
    val notes: String? = null,
    val isFavorite: Boolean = false
)
