package com.ledger.app.domain.usecase

import com.ledger.app.domain.model.PartialPayment
import com.ledger.app.domain.model.Transaction
import com.ledger.app.domain.repository.PartialPaymentRepository
import com.ledger.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Fake implementation of PartialPaymentRepository for testing.
 */
class FakePartialPaymentRepository : PartialPaymentRepository {
    private val payments = mutableMapOf<Long, MutableList<PartialPayment>>()
    private var nextId = 1L

    override suspend fun insert(payment: PartialPayment): Long {
        val id = nextId++
        val paymentWithId = payment.copy(id = id)
        payments.getOrPut(payment.parentTransactionId) { mutableListOf() }.add(paymentWithId)
        return id
    }

    override suspend fun delete(paymentId: Long) {
        payments.values.forEach { list ->
            list.removeIf { it.id == paymentId }
        }
    }

    override fun getByTransaction(transactionId: Long): Flow<List<PartialPayment>> {
        return flowOf(payments[transactionId] ?: emptyList())
    }

    override suspend fun getTotalPaidForTransaction(transactionId: Long): BigDecimal {
        return payments[transactionId]?.fold(BigDecimal.ZERO) { acc, payment ->
            acc + payment.amount
        } ?: BigDecimal.ZERO
    }
}

/**
 * Fake implementation of TransactionRepository for testing.
 */
class FakeTransactionRepository : TransactionRepository {
    private val transactions = mutableMapOf<Long, Transaction>()
    private var nextId = 1L

    override suspend fun insert(transaction: Transaction): Long {
        val id = nextId++
        transactions[id] = transaction.copy(id = id)
        return id
    }

    override suspend fun update(transaction: Transaction) {
        transactions[transaction.id] = transaction
    }

    override suspend fun softDelete(transactionId: Long) {
        transactions.remove(transactionId)
    }

    override suspend fun getById(id: Long): Transaction? {
        return transactions[id]
    }

    override fun getAll(): Flow<List<Transaction>> {
        return flowOf(transactions.values.toList())
    }

    override fun getByCounterparty(counterpartyId: Long): Flow<List<Transaction>> {
        return flowOf(transactions.values.filter { it.counterpartyId == counterpartyId })
    }

    override fun getByDateRange(start: LocalDateTime, end: LocalDateTime): Flow<List<Transaction>> {
        return flowOf(transactions.values.filter { 
            it.transactionDateTime >= start && it.transactionDateTime <= end 
        })
    }

    override fun getOutstandingObligations(): Flow<List<Transaction>> {
        return flowOf(transactions.values.filter { 
            it.status == com.ledger.app.domain.model.TransactionStatus.PENDING ||
            it.status == com.ledger.app.domain.model.TransactionStatus.PARTIALLY_SETTLED
        })
    }

    override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> {
        return flowOf(transactions.values.sortedByDescending { it.transactionDateTime }.take(limit))
    }

    override suspend fun getTotalOwedToUser(): BigDecimal {
        return transactions.values
            .filter { 
                it.direction == com.ledger.app.domain.model.TransactionDirection.GAVE &&
                it.status != com.ledger.app.domain.model.TransactionStatus.CANCELLED
            }
            .fold(BigDecimal.ZERO) { acc, t -> acc + t.remainingDue }
    }

    override suspend fun getTotalUserOwes(): BigDecimal {
        return transactions.values
            .filter { 
                it.direction == com.ledger.app.domain.model.TransactionDirection.RECEIVED &&
                it.status != com.ledger.app.domain.model.TransactionStatus.CANCELLED
            }
            .fold(BigDecimal.ZERO) { acc, t -> acc + t.remainingDue }
    }

    override fun getBillPayments(): Flow<List<Transaction>> {
        return flowOf(transactions.values.filter { 
            it.type == com.ledger.app.domain.model.TransactionType.BILL_PAYMENT 
        })
    }

    override suspend fun searchByNote(query: String): List<Transaction> {
        return transactions.values.filter { 
            it.notes?.contains(query, ignoreCase = true) == true ||
            it.consumerId?.contains(query, ignoreCase = true) == true
        }
    }

    override fun getAllBillPaymentsAndRecharges(): Flow<List<Transaction>> {
        return flowOf(transactions.values.filter { 
            it.type == com.ledger.app.domain.model.TransactionType.BILL_PAYMENT ||
            it.type == com.ledger.app.domain.model.TransactionType.RECHARGE
        })
    }

    override fun getBillPaymentsByForSelf(isForSelf: Boolean): Flow<List<Transaction>> {
        return flowOf(transactions.values.filter { 
            (it.type == com.ledger.app.domain.model.TransactionType.BILL_PAYMENT ||
             it.type == com.ledger.app.domain.model.TransactionType.RECHARGE) &&
            it.isForSelf == isForSelf
        })
    }

    override fun getBillPaymentsByCategoryAndForSelf(category: String, isForSelf: Boolean): Flow<List<Transaction>> {
        return flowOf(transactions.values.filter { 
            (it.type == com.ledger.app.domain.model.TransactionType.BILL_PAYMENT ||
             it.type == com.ledger.app.domain.model.TransactionType.RECHARGE) &&
            it.billCategory?.name == category &&
            it.isForSelf == isForSelf
        })
    }
}


/**
 * Fake implementation of AuditLogRepository for testing.
 */
class FakeAuditLogRepository : com.ledger.app.domain.repository.AuditLogRepository {
    private val logs = mutableListOf<com.ledger.app.domain.model.AuditLog>()
    private var nextId = 1L

    override suspend fun logAction(
        action: com.ledger.app.domain.model.AuditAction,
        entityType: String,
        entityId: Long,
        oldValue: String?,
        newValue: String?,
        details: String?
    ) {
        // Preserve null values as-is for proper testing
        val log = com.ledger.app.domain.model.AuditLog(
            id = nextId++,
            action = action,
            entityType = entityType,
            entityId = entityId,
            oldValue = oldValue,
            newValue = newValue,
            timestamp = java.time.LocalDateTime.now(),
            details = details
        )
        logs.add(log)
    }

    override fun getAuditLogs(): Flow<List<com.ledger.app.domain.model.AuditLog>> {
        return flowOf(logs.toList())
    }

    override fun getRecent(limit: Int): Flow<List<com.ledger.app.domain.model.AuditLog>> {
        return flowOf(logs.sortedByDescending { it.timestamp }.take(limit))
    }

    override fun searchLogs(query: String): Flow<List<com.ledger.app.domain.model.AuditLog>> {
        return flowOf(logs.filter { log ->
            log.details?.contains(query, ignoreCase = true) == true ||
            log.oldValue?.contains(query, ignoreCase = true) == true ||
            log.newValue?.contains(query, ignoreCase = true) == true ||
            log.entityType.contains(query, ignoreCase = true)
        })
    }

    override suspend fun searchLogsSync(query: String): List<com.ledger.app.domain.model.AuditLog> {
        return logs.filter { log ->
            log.details?.contains(query, ignoreCase = true) == true ||
            log.oldValue?.contains(query, ignoreCase = true) == true ||
            log.newValue?.contains(query, ignoreCase = true) == true ||
            log.entityType.contains(query, ignoreCase = true)
        }
    }

    override suspend fun exportAuditLog(): java.io.File {
        throw UnsupportedOperationException("Not implemented for testing")
    }

    override suspend fun getCount(): Int {
        return logs.size
    }

    override fun getByDateRange(startMillis: Long, endMillis: Long): Flow<List<com.ledger.app.domain.model.AuditLog>> {
        return flowOf(logs.filter { log ->
            val logMillis = log.timestamp.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            logMillis in startMillis until endMillis
        })
    }

    override fun getByEntityType(entityType: String): Flow<List<com.ledger.app.domain.model.AuditLog>> {
        return flowOf(logs.filter { it.entityType == entityType })
    }

    override fun getByEntity(entityType: String, entityId: Long): Flow<List<com.ledger.app.domain.model.AuditLog>> {
        return flowOf(logs.filter { it.entityType == entityType && it.entityId == entityId })
    }

    // Test helper methods
    fun getLogsForEntity(entityType: String, entityId: Long): List<com.ledger.app.domain.model.AuditLog> {
        return logs.filter { it.entityType == entityType && it.entityId == entityId }
    }

    fun getAllLogs(): List<com.ledger.app.domain.model.AuditLog> {
        return logs.toList()
    }

    fun clear() {
        logs.clear()
        nextId = 1L
    }
}


/**
 * Fake implementation of ReminderRepository for testing.
 */
class FakeReminderRepository : com.ledger.app.domain.repository.ReminderRepository {
    private val reminders = mutableMapOf<Long, com.ledger.app.domain.model.Reminder>()
    private var nextId = 1L

    override suspend fun insert(reminder: com.ledger.app.domain.model.Reminder): Long {
        val id = nextId++
        reminders[id] = reminder.copy(id = id)
        return id
    }

    override suspend fun update(reminder: com.ledger.app.domain.model.Reminder) {
        reminders[reminder.id] = reminder
    }

    override suspend fun delete(reminderId: Long) {
        reminders.remove(reminderId)
    }

    override suspend fun getById(reminderId: Long): com.ledger.app.domain.model.Reminder? {
        return reminders[reminderId]
    }

    override fun getUpcoming(): Flow<List<com.ledger.app.domain.model.Reminder>> {
        return flowOf(reminders.values.filter { 
            it.status == com.ledger.app.domain.model.ReminderStatus.UPCOMING 
        }.sortedBy { it.dueDateTime })
    }

    override suspend fun getUpcomingCount(): Int {
        return reminders.values.count { 
            it.status == com.ledger.app.domain.model.ReminderStatus.UPCOMING 
        }
    }

    override suspend fun clearRemindersForTransaction(transactionId: Long) {
        val toRemove = reminders.values.filter { 
            it.targetType == com.ledger.app.domain.model.ReminderTargetType.TRANSACTION && 
            it.targetId == transactionId 
        }.map { it.id }
        toRemove.forEach { reminders.remove(it) }
    }

    override suspend fun snoozeReminder(reminderId: Long, newDueDateTime: java.time.LocalDateTime) {
        reminders[reminderId]?.let { reminder ->
            reminders[reminderId] = reminder.copy(
                dueDateTime = newDueDateTime,
                status = com.ledger.app.domain.model.ReminderStatus.SNOOZED
            )
        }
    }

    override suspend fun rescheduleIgnoredReminder(reminderId: Long) {
        reminders[reminderId]?.let { reminder ->
            reminders[reminderId] = reminder.copy(
                dueDateTime = reminder.dueDateTime.plusDays(1),
                ignoredCount = reminder.ignoredCount + 1
            )
        }
    }

    override suspend fun getOverdueReminders(): List<com.ledger.app.domain.model.Reminder> {
        val now = java.time.LocalDateTime.now()
        return reminders.values.filter { 
            it.status == com.ledger.app.domain.model.ReminderStatus.UPCOMING && 
            it.dueDateTime.isBefore(now) 
        }
    }

    override suspend fun rescheduleAllIgnoredReminders() {
        val overdueReminders = getOverdueReminders()
        for (reminder in overdueReminders) {
            rescheduleIgnoredReminder(reminder.id)
        }
    }

    override fun getByTargetType(targetType: com.ledger.app.domain.model.ReminderTargetType): Flow<List<com.ledger.app.domain.model.Reminder>> {
        return flowOf(reminders.values.filter { 
            it.targetType == targetType && 
            it.status == com.ledger.app.domain.model.ReminderStatus.UPCOMING 
        }.sortedBy { it.dueDateTime })
    }

    override suspend fun getRemindersForTransaction(transactionId: Long): List<com.ledger.app.domain.model.Reminder> {
        return reminders.values.filter { 
            it.targetType == com.ledger.app.domain.model.ReminderTargetType.TRANSACTION && 
            it.targetId == transactionId &&
            it.status == com.ledger.app.domain.model.ReminderStatus.UPCOMING
        }
    }

    // Test helper methods
    fun getAllReminders(): List<com.ledger.app.domain.model.Reminder> {
        return reminders.values.toList()
    }

    fun clear() {
        reminders.clear()
        nextId = 1L
    }
}


/**
 * Fake implementation of TaskRepository for testing.
 */
class FakeTaskRepository : com.ledger.app.domain.repository.TaskRepository {
    private val tasks = mutableMapOf<Long, com.ledger.app.domain.model.Task>()
    private val checklistItems = mutableMapOf<Long, MutableList<com.ledger.app.domain.model.ChecklistItem>>()
    private var nextTaskId = 1L
    private var nextChecklistItemId = 1L

    override suspend fun insert(task: com.ledger.app.domain.model.Task): Long {
        val id = nextTaskId++
        val taskWithId = task.copy(id = id)
        tasks[id] = taskWithId
        
        // Insert checklist items if any
        if (task.checklistItems.isNotEmpty()) {
            val items = task.checklistItems.map { item ->
                val itemId = nextChecklistItemId++
                item.copy(id = itemId, taskId = id)
            }
            checklistItems[id] = items.toMutableList()
        }
        return id
    }

    override suspend fun update(task: com.ledger.app.domain.model.Task) {
        tasks[task.id] = task
    }

    override suspend fun delete(taskId: Long) {
        tasks.remove(taskId)
        checklistItems.remove(taskId)
    }

    override suspend fun getById(id: Long): com.ledger.app.domain.model.Task? {
        val task = tasks[id] ?: return null
        val items = checklistItems[id] ?: emptyList()
        return task.copy(checklistItems = items)
    }

    override fun getAll(): Flow<List<com.ledger.app.domain.model.Task>> {
        return flowOf(tasks.values.map { task ->
            task.copy(checklistItems = checklistItems[task.id] ?: emptyList())
        })
    }

    override fun getPendingTasks(): Flow<List<com.ledger.app.domain.model.Task>> {
        return flowOf(tasks.values.filter { 
            it.status == com.ledger.app.domain.model.TaskStatus.PENDING 
        }.map { task ->
            task.copy(checklistItems = checklistItems[task.id] ?: emptyList())
        })
    }

    override fun getCompletedTasks(): Flow<List<com.ledger.app.domain.model.Task>> {
        return flowOf(tasks.values.filter { 
            it.status == com.ledger.app.domain.model.TaskStatus.COMPLETED 
        }.map { task ->
            task.copy(checklistItems = checklistItems[task.id] ?: emptyList())
        })
    }

    override fun getTasksByPriority(priority: com.ledger.app.domain.model.TaskPriority): Flow<List<com.ledger.app.domain.model.Task>> {
        return flowOf(tasks.values.filter { 
            it.priority == priority 
        }.map { task ->
            task.copy(checklistItems = checklistItems[task.id] ?: emptyList())
        })
    }

    override fun getTasksWithDueDate(): Flow<List<com.ledger.app.domain.model.Task>> {
        return flowOf(tasks.values.filter { 
            it.dueDate != null 
        }.sortedBy { it.dueDate }.map { task ->
            task.copy(checklistItems = checklistItems[task.id] ?: emptyList())
        })
    }

    override fun getTasksDueBefore(date: java.time.LocalDateTime): Flow<List<com.ledger.app.domain.model.Task>> {
        return flowOf(tasks.values.filter { 
            it.dueDate != null && it.dueDate.isBefore(date) 
        }.map { task ->
            task.copy(checklistItems = checklistItems[task.id] ?: emptyList())
        })
    }

    override fun getTasksByCounterparty(counterpartyId: Long): Flow<List<com.ledger.app.domain.model.Task>> {
        return flowOf(tasks.values.filter { 
            it.linkedCounterpartyId == counterpartyId 
        }.map { task ->
            task.copy(checklistItems = checklistItems[task.id] ?: emptyList())
        })
    }

    override suspend fun markCompleted(taskId: Long) {
        tasks[taskId]?.let { task ->
            tasks[taskId] = task.copy(
                status = com.ledger.app.domain.model.TaskStatus.COMPLETED,
                completedAt = java.time.LocalDateTime.now()
            )
        }
    }

    override suspend fun markCancelled(taskId: Long) {
        tasks[taskId]?.let { task ->
            tasks[taskId] = task.copy(
                status = com.ledger.app.domain.model.TaskStatus.CANCELLED
            )
        }
    }

    override suspend fun linkTransaction(taskId: Long, transactionId: Long) {
        tasks[taskId]?.let { task ->
            tasks[taskId] = task.copy(linkedTransactionId = transactionId)
        }
    }

    // Checklist operations
    override suspend fun addChecklistItem(item: com.ledger.app.domain.model.ChecklistItem): Long {
        val id = nextChecklistItemId++
        val itemWithId = item.copy(id = id)
        checklistItems.getOrPut(item.taskId) { mutableListOf() }.add(itemWithId)
        return id
    }

    override suspend fun updateChecklistItem(item: com.ledger.app.domain.model.ChecklistItem) {
        checklistItems[item.taskId]?.let { items ->
            val index = items.indexOfFirst { it.id == item.id }
            if (index >= 0) {
                items[index] = item
            }
        }
    }

    override suspend fun deleteChecklistItem(itemId: Long) {
        checklistItems.values.forEach { items ->
            items.removeIf { it.id == itemId }
        }
    }

    override suspend fun setChecklistItemCompleted(itemId: Long, isCompleted: Boolean) {
        checklistItems.values.forEach { items ->
            val index = items.indexOfFirst { it.id == itemId }
            if (index >= 0) {
                items[index] = items[index].copy(isCompleted = isCompleted)
            }
        }
    }

    override fun getChecklistItems(taskId: Long): Flow<List<com.ledger.app.domain.model.ChecklistItem>> {
        return flowOf(checklistItems[taskId] ?: emptyList())
    }

    // Test helper methods
    fun getAllTasks(): List<com.ledger.app.domain.model.Task> {
        return tasks.values.toList()
    }

    fun clear() {
        tasks.clear()
        checklistItems.clear()
        nextTaskId = 1L
        nextChecklistItemId = 1L
    }
}
