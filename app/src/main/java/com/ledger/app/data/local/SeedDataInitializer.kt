package com.ledger.app.data.local

import com.ledger.app.data.local.dao.AccountDao
import com.ledger.app.data.local.dao.CategoryDao
import com.ledger.app.data.local.entity.AccountEntity
import com.ledger.app.data.local.entity.CategoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Initializes seed data on first app launch.
 * Creates default categories and a default Cash account.
 */
@Singleton
class SeedDataInitializer @Inject constructor(
    private val categoryDao: CategoryDao,
    private val accountDao: AccountDao,
    private val preferencesManager: PreferencesManager
) {
    /**
     * Initialize seed data if this is the first launch.
     * Creates default categories (Loan, Bill Payment, Recharge, Rent, Other)
     * and default accounts.
     * Also ensures existing users get new default accounts.
     */
    suspend fun initializeIfNeeded() = withContext(Dispatchers.IO) {
        if (preferencesManager.isFirstLaunch()) {
            createDefaultCategories()
            val cashAccountId = createDefaultAccounts()
            preferencesManager.setDefaultAccountId(cashAccountId)
            preferencesManager.setFirstLaunchComplete()
        } else {
            // For existing users, ensure all default accounts exist
            ensureDefaultAccountsExist()
        }
    }
    
    /**
     * Ensures all default accounts exist for existing users.
     */
    private suspend fun ensureDefaultAccountsExist() {
        val existingAccounts = accountDao.getAllAccounts()
        val existingTypes = existingAccounts.map { it.type }.toSet()
        
        if ("BANK" !in existingTypes) {
            accountDao.insert(AccountEntity(name = "Bank Account", type = "BANK", isActive = true))
        }
        if ("UPI" !in existingTypes) {
            accountDao.insert(AccountEntity(name = "UPI", type = "UPI", isActive = true))
        }
        if ("CARD" !in existingTypes) {
            accountDao.insert(AccountEntity(name = "Credit Card", type = "CARD", isActive = true))
        }
    }

    private suspend fun createDefaultCategories() {
        val defaultCategories = listOf(
            CategoryEntity(
                name = "Loan",
                iconName = "account_balance_wallet",
                colorKey = "blue"
            ),
            CategoryEntity(
                name = "Bill Payment",
                iconName = "receipt_long",
                colorKey = "orange"
            ),
            CategoryEntity(
                name = "Recharge",
                iconName = "phone_android",
                colorKey = "green"
            ),
            CategoryEntity(
                name = "Rent",
                iconName = "home",
                colorKey = "purple"
            ),
            CategoryEntity(
                name = "Other",
                iconName = "more_horiz",
                colorKey = "gray"
            ),
            // Bill-specific categories
            CategoryEntity(
                name = "Electricity",
                iconName = "bolt",
                colorKey = "yellow",
                isBillCategory = true
            ),
            CategoryEntity(
                name = "TV",
                iconName = "tv",
                colorKey = "red",
                isBillCategory = true
            ),
            CategoryEntity(
                name = "Mobile",
                iconName = "smartphone",
                colorKey = "teal",
                isBillCategory = true
            ),
            CategoryEntity(
                name = "Internet",
                iconName = "wifi",
                colorKey = "indigo",
                isBillCategory = true
            )
        )

        defaultCategories.forEach { category ->
            categoryDao.insert(category)
        }
    }

    private suspend fun createDefaultAccounts(): Long {
        // Create default Cash account
        val cashId = accountDao.insert(
            AccountEntity(
                name = "Cash",
                type = "CASH",
                isActive = true
            )
        )
        
        // Create additional default accounts for common payment methods
        accountDao.insert(
            AccountEntity(
                name = "Bank Account",
                type = "BANK",
                isActive = true
            )
        )
        
        accountDao.insert(
            AccountEntity(
                name = "UPI",
                type = "UPI",
                isActive = true
            )
        )
        
        accountDao.insert(
            AccountEntity(
                name = "Credit Card",
                type = "CARD",
                isActive = true
            )
        )
        
        return cashId
    }
}
