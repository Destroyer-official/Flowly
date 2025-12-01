package com.ledger.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ledger.app.data.local.dao.AccountDao
import com.ledger.app.data.local.dao.AuditLogDao
import com.ledger.app.data.local.dao.CategoryDao
import com.ledger.app.data.local.dao.ChecklistItemDao
import com.ledger.app.data.local.dao.CounterpartyDao
import com.ledger.app.data.local.dao.PartialPaymentDao
import com.ledger.app.data.local.dao.ReminderDao
import com.ledger.app.data.local.dao.TaskDao
import com.ledger.app.data.local.dao.TransactionDao
import com.ledger.app.data.local.entity.AccountEntity
import com.ledger.app.data.local.entity.AuditLogEntity
import com.ledger.app.data.local.entity.CategoryEntity
import com.ledger.app.data.local.entity.ChecklistItemEntity
import com.ledger.app.data.local.entity.CounterpartyEntity
import com.ledger.app.data.local.entity.PartialPaymentEntity
import com.ledger.app.data.local.entity.ReminderEntity
import com.ledger.app.data.local.entity.TaskEntity
import com.ledger.app.data.local.entity.TransactionEntity

@Database(
    entities = [
        CounterpartyEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        PartialPaymentEntity::class,
        ReminderEntity::class,
        AuditLogEntity::class,
        TaskEntity::class,
        ChecklistItemEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class LedgerDatabase : RoomDatabase() {
    abstract fun counterpartyDao(): CounterpartyDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun partialPaymentDao(): PartialPaymentDao
    abstract fun reminderDao(): ReminderDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun taskDao(): TaskDao
    abstract fun checklistItemDao(): ChecklistItemDao

    companion object {
        const val DATABASE_NAME = "ledger_database"
    }
}
