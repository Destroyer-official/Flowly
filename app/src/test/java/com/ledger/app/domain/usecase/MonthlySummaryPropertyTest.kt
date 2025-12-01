package com.ledger.app.domain.usecase

import com.ledger.app.domain.model.Category
import com.ledger.app.domain.model.Counterparty
import com.ledger.app.domain.model.Transaction
import com.ledger.app.domain.model.TransactionDirection
import com.ledger.app.domain.model.TransactionStatus
import com.ledger.app.domain.model.TransactionType
import com.ledger.app.domain.repository.CategoryRepository
import com.ledger.app.domain.repository.CounterpartyRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Property-based tests for monthly summary aggregation.
 * 
 * Tests Property 8 from the design document.
 */
class MonthlySummaryPropertyTest : FunSpec({

    /**
     * Generator for valid positive amounts (in cents, converted to BigDecimal).
     */
    fun arbPositiveAmount(): Arb<BigDecimal> = Arb.long(1L, 1_000_000_00L).map { cents ->
        BigDecimal(cents).divide(BigDecimal(100))
    }

    /**
     * Generator for transactions within a specific month.
     */
    fun arbTransactionInMonth(year: Int, month: Int): Arb<Transaction> = Arb.bind(
        arbPositiveAmount(),
        Arb.enum<TransactionDirection>(),
        Arb.enum<TransactionType>(),
        Arb.enum<TransactionStatus>(),
        Arb.int(1..28) // Safe day range for all months
    ) { amount, direction, type, status, day ->
        Transaction(
            id = 0,
            direction = direction,
            type = type,
            amount = amount,
            accountId = 1,
            counterpartyId = 1,
            categoryId = 1,
            transactionDateTime = LocalDateTime.of(year, month, day, 12, 0),
            status = status,
            notes = null,
            remainingDue = if (status == TransactionStatus.SETTLED) BigDecimal.ZERO else amount
        )
    }

    /**
     * **Feature: offline-ledger-app, Property 8: Monthly Summary Aggregation**
     * 
     * For any month, the monthly totals (outflow, inflow, net) should equal the sum 
     * of individual transaction amounts within that period, excluding cancelled and 
     * soft-deleted transactions.
     * 
     * **Validates: Requirements 7.1**
     */
    test("Property 8 - Monthly outflow equals sum of GAVE transactions") {
        checkAll(100, Arb.list(arbTransactionInMonth(2025, 11), 0..20)) { transactions ->
            runTest {
                val fakeTransactionRepo = FakeTransactionRepository()
                val fakeCategoryRepo = FakeCategoryRepository()
                val fakeCounterpartyRepo = FakeCounterpartyRepository()
                
                // Insert all transactions
                transactions.forEach { transaction ->
                    fakeTransactionRepo.insert(transaction)
                }
                
                val useCase = GetMonthlyAnalyticsUseCase(
                    fakeTransactionRepo,
                    fakeCategoryRepo,
                    fakeCounterpartyRepo
                )
                
                val analyticsData = useCase(2025, 11)
                
                // Calculate expected outflow (excluding cancelled)
                val expectedOutflow = transactions
                    .filter { it.direction == TransactionDirection.GAVE }
                    .filter { it.status != TransactionStatus.CANCELLED }
                    .fold(BigDecimal.ZERO) { acc, t -> acc + t.amount }
                
                analyticsData.monthlySummary.totalOutflow.compareTo(expectedOutflow) shouldBe 0
            }
        }
    }

    test("Property 8 - Monthly inflow equals sum of RECEIVED transactions") {
        checkAll(100, Arb.list(arbTransactionInMonth(2025, 11), 0..20)) { transactions ->
            runTest {
                val fakeTransactionRepo = FakeTransactionRepository()
                val fakeCategoryRepo = FakeCategoryRepository()
                val fakeCounterpartyRepo = FakeCounterpartyRepository()
                
                // Insert all transactions
                transactions.forEach { transaction ->
                    fakeTransactionRepo.insert(transaction)
                }
                
                val useCase = GetMonthlyAnalyticsUseCase(
                    fakeTransactionRepo,
                    fakeCategoryRepo,
                    fakeCounterpartyRepo
                )
                
                val analyticsData = useCase(2025, 11)
                
                // Calculate expected inflow (excluding cancelled)
                val expectedInflow = transactions
                    .filter { it.direction == TransactionDirection.RECEIVED }
                    .filter { it.status != TransactionStatus.CANCELLED }
                    .fold(BigDecimal.ZERO) { acc, t -> acc + t.amount }
                
                analyticsData.monthlySummary.totalInflow.compareTo(expectedInflow) shouldBe 0
            }
        }
    }

    test("Property 8 - Net balance equals inflow minus outflow") {
        checkAll(100, Arb.list(arbTransactionInMonth(2025, 11), 0..20)) { transactions ->
            runTest {
                val fakeTransactionRepo = FakeTransactionRepository()
                val fakeCategoryRepo = FakeCategoryRepository()
                val fakeCounterpartyRepo = FakeCounterpartyRepository()
                
                // Insert all transactions
                transactions.forEach { transaction ->
                    fakeTransactionRepo.insert(transaction)
                }
                
                val useCase = GetMonthlyAnalyticsUseCase(
                    fakeTransactionRepo,
                    fakeCategoryRepo,
                    fakeCounterpartyRepo
                )
                
                val analyticsData = useCase(2025, 11)
                
                // Calculate expected values (excluding cancelled)
                val nonCancelledTransactions = transactions.filter { it.status != TransactionStatus.CANCELLED }
                
                val expectedOutflow = nonCancelledTransactions
                    .filter { it.direction == TransactionDirection.GAVE }
                    .fold(BigDecimal.ZERO) { acc, t -> acc + t.amount }
                
                val expectedInflow = nonCancelledTransactions
                    .filter { it.direction == TransactionDirection.RECEIVED }
                    .fold(BigDecimal.ZERO) { acc, t -> acc + t.amount }
                
                val expectedNet = expectedInflow - expectedOutflow
                
                analyticsData.monthlySummary.netBalance.compareTo(expectedNet) shouldBe 0
            }
        }
    }

    test("Property 8 - Cancelled transactions excluded from monthly summary") {
        checkAll(100, arbPositiveAmount()) { amount ->
            runTest {
                val fakeTransactionRepo = FakeTransactionRepository()
                val fakeCategoryRepo = FakeCategoryRepository()
                val fakeCounterpartyRepo = FakeCounterpartyRepository()
                
                // Insert only cancelled transactions
                val cancelledGave = Transaction(
                    id = 0,
                    direction = TransactionDirection.GAVE,
                    type = TransactionType.LOAN,
                    amount = amount,
                    accountId = 1,
                    counterpartyId = 1,
                    categoryId = 1,
                    transactionDateTime = LocalDateTime.of(2025, 11, 15, 12, 0),
                    status = TransactionStatus.CANCELLED,
                    notes = null,
                    remainingDue = amount
                )
                
                val cancelledReceived = Transaction(
                    id = 0,
                    direction = TransactionDirection.RECEIVED,
                    type = TransactionType.LOAN,
                    amount = amount,
                    accountId = 1,
                    counterpartyId = 1,
                    categoryId = 1,
                    transactionDateTime = LocalDateTime.of(2025, 11, 20, 12, 0),
                    status = TransactionStatus.CANCELLED,
                    notes = null,
                    remainingDue = amount
                )
                
                fakeTransactionRepo.insert(cancelledGave)
                fakeTransactionRepo.insert(cancelledReceived)
                
                val useCase = GetMonthlyAnalyticsUseCase(
                    fakeTransactionRepo,
                    fakeCategoryRepo,
                    fakeCounterpartyRepo
                )
                
                val analyticsData = useCase(2025, 11)
                
                // All values should be zero since all transactions are cancelled
                analyticsData.monthlySummary.totalOutflow.compareTo(BigDecimal.ZERO) shouldBe 0
                analyticsData.monthlySummary.totalInflow.compareTo(BigDecimal.ZERO) shouldBe 0
                analyticsData.monthlySummary.netBalance.compareTo(BigDecimal.ZERO) shouldBe 0
            }
        }
    }

    test("Property 8 - Transactions outside month excluded from summary") {
        checkAll(100, arbPositiveAmount()) { amount ->
            runTest {
                val fakeTransactionRepo = FakeTransactionRepository()
                val fakeCategoryRepo = FakeCategoryRepository()
                val fakeCounterpartyRepo = FakeCounterpartyRepository()
                
                // Insert transaction in October (not November)
                val octoberTransaction = Transaction(
                    id = 0,
                    direction = TransactionDirection.GAVE,
                    type = TransactionType.LOAN,
                    amount = amount,
                    accountId = 1,
                    counterpartyId = 1,
                    categoryId = 1,
                    transactionDateTime = LocalDateTime.of(2025, 10, 15, 12, 0),
                    status = TransactionStatus.PENDING,
                    notes = null,
                    remainingDue = amount
                )
                
                // Insert transaction in December (not November)
                val decemberTransaction = Transaction(
                    id = 0,
                    direction = TransactionDirection.RECEIVED,
                    type = TransactionType.LOAN,
                    amount = amount,
                    accountId = 1,
                    counterpartyId = 1,
                    categoryId = 1,
                    transactionDateTime = LocalDateTime.of(2025, 12, 5, 12, 0),
                    status = TransactionStatus.PENDING,
                    notes = null,
                    remainingDue = amount
                )
                
                fakeTransactionRepo.insert(octoberTransaction)
                fakeTransactionRepo.insert(decemberTransaction)
                
                val useCase = GetMonthlyAnalyticsUseCase(
                    fakeTransactionRepo,
                    fakeCategoryRepo,
                    fakeCounterpartyRepo
                )
                
                val analyticsData = useCase(2025, 11)
                
                // All values should be zero since no transactions are in November
                analyticsData.monthlySummary.totalOutflow.compareTo(BigDecimal.ZERO) shouldBe 0
                analyticsData.monthlySummary.totalInflow.compareTo(BigDecimal.ZERO) shouldBe 0
                analyticsData.monthlySummary.netBalance.compareTo(BigDecimal.ZERO) shouldBe 0
            }
        }
    }
})

/**
 * Fake implementation of CategoryRepository for testing.
 */
private class FakeCategoryRepository : CategoryRepository {
    private val categories = mutableMapOf<Long, Category>()
    private var nextId = 1L

    init {
        // Add some default categories
        categories[1] = Category(1, "Loan", "loan", "blue")
        categories[2] = Category(2, "Bill", "bill", "green")
        categories[3] = Category(3, "Recharge", "recharge", "orange")
        nextId = 4
    }

    override suspend fun insert(category: Category): Long {
        val id = if (category.id == 0L) nextId++ else category.id
        categories[id] = category.copy(id = id)
        return id
    }

    override suspend fun getById(id: Long): Category? {
        return categories[id]
    }

    override fun getAll(): Flow<List<Category>> {
        return flowOf(categories.values.toList())
    }
}

/**
 * Fake implementation of CounterpartyRepository for testing.
 */
private class FakeCounterpartyRepository : CounterpartyRepository {
    private val counterparties = mutableMapOf<Long, Counterparty>()
    private var nextId = 1L

    init {
        // Add some default counterparties
        counterparties[1] = Counterparty(1, "John Doe", null, null, false)
        counterparties[2] = Counterparty(2, "Jane Smith", null, null, false)
        nextId = 3
    }

    override suspend fun insert(counterparty: Counterparty): Long {
        val id = if (counterparty.id == 0L) nextId++ else counterparty.id
        counterparties[id] = counterparty.copy(id = id)
        return id
    }

    override suspend fun update(counterparty: Counterparty) {
        counterparties[counterparty.id] = counterparty
    }

    override suspend fun delete(counterpartyId: Long) {
        counterparties.remove(counterpartyId)
    }

    override suspend fun getById(id: Long): Counterparty? {
        return counterparties[id]
    }

    override fun getAll(): Flow<List<Counterparty>> {
        return flowOf(counterparties.values.toList())
    }

    override fun search(query: String): Flow<List<Counterparty>> {
        return flowOf(counterparties.values.filter { 
            it.displayName.contains(query, ignoreCase = true) ||
            it.phoneNumber?.contains(query) == true
        })
    }

    override fun getAllWithBalances(): Flow<List<com.ledger.app.domain.model.CounterpartyWithBalance>> {
        return flowOf(emptyList())
    }
}
