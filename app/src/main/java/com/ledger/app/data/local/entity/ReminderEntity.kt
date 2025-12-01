package com.ledger.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val targetType: String, // TRANSACTION, COUNTERPARTY, BILL, GENERIC
    val targetId: Long? = null,
    val title: String,
    val description: String? = null,
    val dueDateTime: Long,
    val repeatPattern: String = "NONE", // NONE, DAILY, WEEKLY, MONTHLY
    val status: String = "UPCOMING", // UPCOMING, DONE, SNOOZED, CANCELLED
    val ignoredCount: Int = 0 // Track how many times ignored for auto-reschedule
)
