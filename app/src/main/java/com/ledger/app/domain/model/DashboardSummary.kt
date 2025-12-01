package com.ledger.app.domain.model

import java.math.BigDecimal

/**
 * Domain model representing the dashboard summary data.
 * 
 * Requirements: 9.1, 9.2, 9.3
 */
data class DashboardSummary(
    val totalOwedToUser: BigDecimal,
    val totalUserOwes: BigDecimal,
    val upcomingRemindersCount: Int,
    val recentTransactions: List<Transaction>,
    val topDebtors: List<CounterpartyWithBalance> = emptyList(),
    val monthlyBillSummary: Map<BillCategory, BigDecimal> = emptyMap()
)
