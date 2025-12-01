package com.ledger.app.domain.model

import java.time.LocalDateTime

/**
 * Domain model representing a reminder for financial obligations.
 */
data class Reminder(
    val id: Long = 0,
    val targetType: ReminderTargetType,
    val targetId: Long? = null,
    val title: String,
    val description: String? = null,
    val dueDateTime: LocalDateTime,
    val repeatPattern: RepeatPattern = RepeatPattern.NONE,
    val status: ReminderStatus = ReminderStatus.UPCOMING,
    val ignoredCount: Int = 0 // Track how many times ignored for auto-reschedule
)
