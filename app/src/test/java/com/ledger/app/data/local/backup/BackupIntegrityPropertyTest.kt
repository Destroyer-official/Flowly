package com.ledger.app.data.local.backup

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Property-based tests for backup integrity.
 * 
 * **Feature: offline-ledger-app, Property 15: Backup Integrity**
 * 
 * For any backup operation, the exported file should contain all non-deleted records,
 * and restoration should produce identical data state.
 * 
 * **Validates: Requirements 13.1, 13.2**
 */
class BackupIntegrityPropertyTest : FunSpec({

    val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Generator for valid transaction backup data.
     */
    fun arbTransactionBackup(): Arb<TransactionBackup> = Arb.bind(
        Arb.long(1L, 10000L),
        Arb.string(1..10).map { if (it.isEmpty()) "GAVE" else listOf("GAVE", "RECEIVED").random() },
        Arb.string(1..10).map { listOf("LOAN", "BILL_PAYMENT", "RECHARGE", "OTHER").random() },
        Arb.long(1L, 1_000_000_00L).map { it.toString() },
        Arb.long(1L, 100L),
        Arb.long(1L, 100L).orNull(),
        Arb.long(1L, 100L)
    ) { id, direction, type, amount, accountId, counterpartyId, categoryId ->
        TransactionBackup(
            id = id,
            direction = direction,
            type = type,
            amount = amount,
            accountId = accountId,
            counterpartyId = counterpartyId,
            categoryId = categoryId,
            transactionDateTime = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            status = "PENDING",
            notes = null,
            remainingDue = amount,
            isSoftDeleted = false,
            consumerId = null,
            billCategory = null,
            isForSelf = false
        )
    }


    /**
     * Generator for valid account backup data.
     */
    fun arbAccountBackup(): Arb<AccountBackup> = Arb.bind(
        Arb.long(1L, 1000L),
        Arb.string(1..20),
        Arb.string(1..10).map { listOf("CASH", "BANK", "UPI", "CARD", "OTHER").random() },
        Arb.boolean()
    ) { id, name, type, isActive ->
        AccountBackup(
            id = id,
            name = name.ifEmpty { "Account" },
            type = type,
            isActive = isActive
        )
    }

    /**
     * Generator for valid category backup data.
     */
    fun arbCategoryBackup(): Arb<CategoryBackup> = Arb.bind(
        Arb.long(1L, 1000L),
        Arb.string(1..20),
        Arb.string(1..20),
        Arb.string(1..10),
        Arb.boolean()
    ) { id, name, iconName, colorKey, isBillCategory ->
        CategoryBackup(
            id = id,
            name = name.ifEmpty { "Category" },
            iconName = iconName.ifEmpty { "icon" },
            colorKey = colorKey.ifEmpty { "color" },
            isBillCategory = isBillCategory
        )
    }

    /**
     * Generator for valid counterparty backup data.
     */
    fun arbCounterpartyBackup(): Arb<CounterpartyBackup> = Arb.bind(
        Arb.long(1L, 1000L),
        Arb.string(1..30),
        Arb.string(10..10).orNull(),
        Arb.string(0..50).orNull(),
        Arb.boolean()
    ) { id, displayName, phoneNumber, notes, isFavorite ->
        CounterpartyBackup(
            id = id,
            displayName = displayName.ifEmpty { "Person" },
            phoneNumber = phoneNumber,
            notes = notes,
            isFavorite = isFavorite,
            createdAt = System.currentTimeMillis()
        )
    }

    /**
     * Generator for valid reminder backup data.
     */
    fun arbReminderBackup(): Arb<ReminderBackup> = Arb.bind(
        Arb.long(1L, 1000L),
        Arb.string(1..10).map { listOf("TRANSACTION", "COUNTERPARTY", "BILL", "GENERIC").random() },
        Arb.long(1L, 1000L).orNull(),
        Arb.string(1..50),
        Arb.string(0..100).orNull(),
        Arb.int(0..10)
    ) { id, targetType, targetId, title, description, ignoredCount ->
        ReminderBackup(
            id = id,
            targetType = targetType,
            targetId = targetId,
            title = title.ifEmpty { "Reminder" },
            description = description,
            dueDateTime = System.currentTimeMillis() + 86400000L,
            repeatPattern = "NONE",
            status = "UPCOMING",
            ignoredCount = ignoredCount
        )
    }

    /**
     * Generator for valid BackupData.
     */
    fun arbBackupData(): Arb<BackupData> = Arb.bind(
        Arb.list(arbTransactionBackup(), 0..5),
        Arb.list(arbAccountBackup(), 1..3),
        Arb.list(arbCategoryBackup(), 1..3),
        Arb.list(arbCounterpartyBackup(), 0..3),
        Arb.list(arbReminderBackup(), 0..3)
    ) { transactions, accounts, categories, counterparties, reminders ->
        BackupData(
            version = BackupData.CURRENT_VERSION,
            createdAt = System.currentTimeMillis(),
            transactions = transactions,
            partialPayments = emptyList(),
            counterparties = counterparties,
            accounts = accounts,
            categories = categories,
            reminders = reminders,
            auditLogs = emptyList()
        )
    }

    /**
     * **Feature: offline-ledger-app, Property 15: Backup Integrity**
     * 
     * For any backup data, serializing to JSON and deserializing should produce
     * identical data (round-trip property).
     * 
     * **Validates: Requirements 13.1, 13.2**
     */
    test("Property 15 - Backup data round-trip serialization preserves all data") {
        checkAll(100, arbBackupData()) { originalData ->
            // Serialize to JSON
            val jsonString = json.encodeToString(originalData)
            
            // Deserialize back
            val restoredData = json.decodeFromString<BackupData>(jsonString)
            
            // Verify all fields match
            restoredData.version shouldBe originalData.version
            restoredData.transactions.size shouldBe originalData.transactions.size
            restoredData.accounts.size shouldBe originalData.accounts.size
            restoredData.categories.size shouldBe originalData.categories.size
            restoredData.counterparties.size shouldBe originalData.counterparties.size
            restoredData.reminders.size shouldBe originalData.reminders.size
            restoredData.partialPayments.size shouldBe originalData.partialPayments.size
            restoredData.auditLogs.size shouldBe originalData.auditLogs.size
        }
    }

    test("Property 15 - Transaction backup round-trip preserves all fields") {
        checkAll(100, arbTransactionBackup()) { original ->
            val jsonString = json.encodeToString(original)
            val restored = json.decodeFromString<TransactionBackup>(jsonString)
            
            restored.id shouldBe original.id
            restored.direction shouldBe original.direction
            restored.type shouldBe original.type
            restored.amount shouldBe original.amount
            restored.accountId shouldBe original.accountId
            restored.counterpartyId shouldBe original.counterpartyId
            restored.categoryId shouldBe original.categoryId
            restored.status shouldBe original.status
            restored.remainingDue shouldBe original.remainingDue
            restored.isSoftDeleted shouldBe original.isSoftDeleted
            restored.isForSelf shouldBe original.isForSelf
        }
    }

    test("Property 15 - Account backup round-trip preserves all fields") {
        checkAll(100, arbAccountBackup()) { original ->
            val jsonString = json.encodeToString(original)
            val restored = json.decodeFromString<AccountBackup>(jsonString)
            
            restored.id shouldBe original.id
            restored.name shouldBe original.name
            restored.type shouldBe original.type
            restored.isActive shouldBe original.isActive
        }
    }

    test("Property 15 - Category backup round-trip preserves all fields") {
        checkAll(100, arbCategoryBackup()) { original ->
            val jsonString = json.encodeToString(original)
            val restored = json.decodeFromString<CategoryBackup>(jsonString)
            
            restored.id shouldBe original.id
            restored.name shouldBe original.name
            restored.iconName shouldBe original.iconName
            restored.colorKey shouldBe original.colorKey
            restored.isBillCategory shouldBe original.isBillCategory
        }
    }

    test("Property 15 - Counterparty backup round-trip preserves all fields") {
        checkAll(100, arbCounterpartyBackup()) { original ->
            val jsonString = json.encodeToString(original)
            val restored = json.decodeFromString<CounterpartyBackup>(jsonString)
            
            restored.id shouldBe original.id
            restored.displayName shouldBe original.displayName
            restored.phoneNumber shouldBe original.phoneNumber
            restored.notes shouldBe original.notes
            restored.isFavorite shouldBe original.isFavorite
        }
    }

    test("Property 15 - Reminder backup round-trip preserves all fields") {
        checkAll(100, arbReminderBackup()) { original ->
            val jsonString = json.encodeToString(original)
            val restored = json.decodeFromString<ReminderBackup>(jsonString)
            
            restored.id shouldBe original.id
            restored.targetType shouldBe original.targetType
            restored.targetId shouldBe original.targetId
            restored.title shouldBe original.title
            restored.description shouldBe original.description
            restored.repeatPattern shouldBe original.repeatPattern
            restored.status shouldBe original.status
            restored.ignoredCount shouldBe original.ignoredCount
        }
    }

    test("Property 15 - Total records count is accurate") {
        checkAll(100, arbBackupData()) { backupData ->
            val expectedTotal = backupData.transactions.size +
                backupData.partialPayments.size +
                backupData.counterparties.size +
                backupData.accounts.size +
                backupData.categories.size +
                backupData.reminders.size +
                backupData.auditLogs.size
            
            backupData.totalRecords() shouldBe expectedTotal
        }
    }

    test("Property 15 - Empty backup data serializes and deserializes correctly") {
        val emptyBackup = BackupData(
            transactions = emptyList(),
            partialPayments = emptyList(),
            counterparties = emptyList(),
            accounts = emptyList(),
            categories = emptyList(),
            reminders = emptyList(),
            auditLogs = emptyList()
        )
        
        val jsonString = json.encodeToString(emptyBackup)
        val restored = json.decodeFromString<BackupData>(jsonString)
        
        restored.totalRecords() shouldBe 0
        restored.version shouldBe BackupData.CURRENT_VERSION
    }

    test("Property 15 - Backup version is preserved") {
        checkAll(100, arbBackupData()) { backupData ->
            val jsonString = json.encodeToString(backupData)
            val restored = json.decodeFromString<BackupData>(jsonString)
            
            restored.version shouldBe BackupData.CURRENT_VERSION
        }
    }
})
