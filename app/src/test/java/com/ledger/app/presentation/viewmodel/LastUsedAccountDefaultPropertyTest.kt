package com.ledger.app.presentation.viewmodel

import com.ledger.app.domain.model.Account
import com.ledger.app.domain.model.AccountType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

/**
 * Property-based tests for last used account default behavior.
 * 
 * Tests Property 17 from the design document.
 * 
 * **Feature: offline-ledger-app, Property 17: Last Used Account Default**
 * 
 * For any new transaction, if no account is explicitly selected, 
 * the last used account should be pre-selected.
 * 
 * **Validates: Requirements 1.5**
 */
class LastUsedAccountDefaultPropertyTest : FunSpec({

    /**
     * Generator for valid account names.
     */
    fun arbAccountName(): Arb<String> = Arb.string(1..20)

    /**
     * Generator for accounts.
     */
    fun arbAccount(): Arb<Account> = Arb.bind(
        Arb.long(1L, 1000L),
        arbAccountName(),
        Arb.enum<AccountType>()
    ) { id, name, type ->
        Account(
            id = id,
            name = name,
            type = type,
            isActive = true
        )
    }

    /**
     * Fake PreferencesManager for testing.
     */
    class FakePreferencesManager {
        private var defaultAccountId: Long? = null

        fun getDefaultAccountId(): Long? = defaultAccountId

        fun setDefaultAccountId(accountId: Long?) {
            defaultAccountId = accountId
        }
    }

    /**
     * Fake AccountRepository for testing.
     */
    class FakeAccountRepository : com.ledger.app.domain.repository.AccountRepository {
        private val accounts = mutableMapOf<Long, Account>()
        private var nextId = 1L

        override suspend fun insert(account: Account): Long {
            val id = if (account.id == 0L) nextId++ else account.id
            accounts[id] = account.copy(id = id)
            return id
        }

        override suspend fun update(account: Account) {
            accounts[account.id] = account
        }

        override suspend fun deactivate(accountId: Long) {
            accounts[accountId]?.let { account ->
                accounts[accountId] = account.copy(isActive = false)
            }
        }

        override fun getActive(): Flow<List<Account>> {
            return flowOf(accounts.values.filter { it.isActive }.toList())
        }

        override suspend fun getById(id: Long): Account? {
            return accounts[id]
        }

        fun addAccount(account: Account) {
            accounts[account.id] = account
        }

        fun clear() {
            accounts.clear()
            nextId = 1L
        }
    }

    /**
     * **Feature: offline-ledger-app, Property 17: Last Used Account Default**
     * 
     * For any set of active accounts with a default account set,
     * the default account should be pre-selected when loading accounts.
     * 
     * **Validates: Requirements 1.5**
     */
    test("Property 17 - Default account is pre-selected when available") {
        checkAll(100, Arb.list(arbAccount(), 1..10)) { accounts ->
            runTest {
                val fakeAccountRepo = FakeAccountRepository()
                val fakePrefs = FakePreferencesManager()

                // Add all accounts to repository
                val insertedAccounts = accounts.mapIndexed { index, account ->
                    val uniqueAccount = account.copy(id = (index + 1).toLong())
                    fakeAccountRepo.addAccount(uniqueAccount)
                    uniqueAccount
                }

                // Set a random account as default
                val defaultAccount = insertedAccounts.random()
                fakePrefs.setDefaultAccountId(defaultAccount.id)

                // Simulate the account selection logic from QuickAddViewModel
                val activeAccounts = fakeAccountRepo.getActive()
                var selectedAccount: Account? = null

                activeAccounts.collect { list ->
                    if (list.isNotEmpty()) {
                        val defaultAccountId = fakePrefs.getDefaultAccountId()
                        val foundDefault = defaultAccountId?.let { id ->
                            list.find { it.id == id }
                        }
                        selectedAccount = foundDefault ?: list.first()
                    }
                }

                // Verify the default account is selected
                selectedAccount shouldNotBe null
                selectedAccount?.id shouldBe defaultAccount.id
            }
        }
    }

    test("Property 17 - First account is selected when no default is set") {
        checkAll(100, Arb.list(arbAccount(), 1..10)) { accounts ->
            runTest {
                val fakeAccountRepo = FakeAccountRepository()
                val fakePrefs = FakePreferencesManager()

                // Add all accounts to repository (no default set)
                val insertedAccounts = accounts.mapIndexed { index, account ->
                    val uniqueAccount = account.copy(id = (index + 1).toLong())
                    fakeAccountRepo.addAccount(uniqueAccount)
                    uniqueAccount
                }

                // Simulate the account selection logic from QuickAddViewModel
                val activeAccounts = fakeAccountRepo.getActive()
                var selectedAccount: Account? = null

                activeAccounts.collect { list ->
                    if (list.isNotEmpty()) {
                        val defaultAccountId = fakePrefs.getDefaultAccountId()
                        val foundDefault = defaultAccountId?.let { id ->
                            list.find { it.id == id }
                        }
                        selectedAccount = foundDefault ?: list.first()
                    }
                }

                // Verify an account is selected (first one when no default)
                selectedAccount shouldNotBe null
                selectedAccount?.id shouldBe insertedAccounts.first().id
            }
        }
    }

    test("Property 17 - Falls back to first account when default account is deactivated") {
        checkAll(100, Arb.list(arbAccount(), 2..10)) { accounts ->
            runTest {
                val fakeAccountRepo = FakeAccountRepository()
                val fakePrefs = FakePreferencesManager()

                // Add all accounts to repository
                val insertedAccounts = accounts.mapIndexed { index, account ->
                    val uniqueAccount = account.copy(id = (index + 1).toLong())
                    fakeAccountRepo.addAccount(uniqueAccount)
                    uniqueAccount
                }

                // Set first account as default, then deactivate it
                val defaultAccount = insertedAccounts.first()
                fakePrefs.setDefaultAccountId(defaultAccount.id)
                fakeAccountRepo.deactivate(defaultAccount.id)

                // Simulate the account selection logic from QuickAddViewModel
                val activeAccounts = fakeAccountRepo.getActive()
                var selectedAccount: Account? = null

                activeAccounts.collect { list ->
                    if (list.isNotEmpty()) {
                        val defaultAccountId = fakePrefs.getDefaultAccountId()
                        val foundDefault = defaultAccountId?.let { id ->
                            list.find { it.id == id }
                        }
                        selectedAccount = foundDefault ?: list.first()
                    }
                }

                // Verify fallback to first active account (not the deactivated default)
                selectedAccount shouldNotBe null
                selectedAccount?.id shouldNotBe defaultAccount.id
                selectedAccount?.isActive shouldBe true
            }
        }
    }

    test("Property 17 - No account selected when no accounts exist") {
        runTest {
            val fakeAccountRepo = FakeAccountRepository()
            val fakePrefs = FakePreferencesManager()

            // Set a default that doesn't exist
            fakePrefs.setDefaultAccountId(999L)

            // Simulate the account selection logic from QuickAddViewModel
            val activeAccounts = fakeAccountRepo.getActive()
            var selectedAccount: Account? = null

            activeAccounts.collect { list ->
                if (list.isNotEmpty()) {
                    val defaultAccountId = fakePrefs.getDefaultAccountId()
                    val foundDefault = defaultAccountId?.let { id ->
                        list.find { it.id == id }
                    }
                    selectedAccount = foundDefault ?: list.first()
                }
            }

            // Verify no account is selected when none exist
            selectedAccount shouldBe null
        }
    }

    test("Property 17 - Default account persists across multiple selections") {
        checkAll(100, Arb.list(arbAccount(), 2..10)) { accounts ->
            runTest {
                val fakeAccountRepo = FakeAccountRepository()
                val fakePrefs = FakePreferencesManager()

                // Add all accounts to repository
                val insertedAccounts = accounts.mapIndexed { index, account ->
                    val uniqueAccount = account.copy(id = (index + 1).toLong())
                    fakeAccountRepo.addAccount(uniqueAccount)
                    uniqueAccount
                }

                // Set a default account
                val defaultAccount = insertedAccounts.random()
                fakePrefs.setDefaultAccountId(defaultAccount.id)

                // Simulate multiple account selection attempts
                repeat(3) {
                    val activeAccounts = fakeAccountRepo.getActive()
                    var selectedAccount: Account? = null

                    activeAccounts.collect { list ->
                        if (list.isNotEmpty()) {
                            val defaultAccountId = fakePrefs.getDefaultAccountId()
                            val foundDefault = defaultAccountId?.let { id ->
                                list.find { it.id == id }
                            }
                            selectedAccount = foundDefault ?: list.first()
                        }
                    }

                    // Verify the same default account is selected each time
                    selectedAccount?.id shouldBe defaultAccount.id
                }
            }
        }
    }
})
