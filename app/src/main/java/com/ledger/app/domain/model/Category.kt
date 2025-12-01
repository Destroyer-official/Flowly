package com.ledger.app.domain.model

/**
 * Domain model representing a transaction category.
 */
data class Category(
    val id: Long = 0,
    val name: String,
    val iconName: String,
    val colorKey: String
)
