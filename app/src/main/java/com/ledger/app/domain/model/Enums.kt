package com.ledger.app.domain.model

/**
 * Direction of a transaction from the user's perspective.
 */
enum class TransactionDirection {
    GAVE,      // User gave money to counterparty
    RECEIVED   // User received money from counterparty
}

/**
 * Type/category of transaction.
 */
enum class TransactionType {
    LOAN,
    BILL_PAYMENT,
    RECHARGE,
    OTHER
}

/**
 * Status of a transaction based on settlement progress.
 */
enum class TransactionStatus {
    PENDING,            // No payments made yet
    PARTIALLY_SETTLED,  // Some payments made, remaining_due > 0
    SETTLED,            // Fully paid, remaining_due = 0
    CANCELLED           // Transaction cancelled, excluded from calculations
}

/**
 * Direction of a partial payment.
 */
enum class PaymentDirection {
    FROM_COUNTERPARTY,  // Counterparty paying back to user
    TO_COUNTERPARTY     // User paying back to counterparty
}

/**
 * Payment method used for transactions and partial payments.
 */
enum class PaymentMethod {
    CASH,
    UPI,
    BANK,
    CARD,
    OTHER
}

/**
 * Type of account/wallet.
 */
enum class AccountType {
    CASH,
    BANK,
    UPI,
    CARD,
    OTHER
}

/**
 * Target type for reminders.
 */
enum class ReminderTargetType {
    TRANSACTION,
    COUNTERPARTY,
    BILL,
    GENERIC
}

/**
 * Bill category for utility payments.
 */
enum class BillCategory {
    ELECTRICITY,
    TV,
    MOBILE,
    INTERNET,
    OTHER
}

/**
 * Repeat pattern for reminders.
 */
enum class RepeatPattern {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY
}

/**
 * Status of a reminder.
 */
enum class ReminderStatus {
    UPCOMING,
    DONE,
    SNOOZED,
    CANCELLED
}
