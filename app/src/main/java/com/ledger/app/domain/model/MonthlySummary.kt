package com.ledger.app.domain.model

import java.math.BigDecimal

/**
 * Domain model representing monthly analytics summary.
 */
data class MonthlySummary(
    val year: Int,
    val month: Int,
    val totalOutflow: BigDecimal,
    val totalInflow: BigDecimal,
    val netBalance: BigDecimal
)
