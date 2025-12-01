package com.ledger.app.domain.usecase

import com.ledger.app.domain.model.Task
import com.ledger.app.domain.model.TaskPriority
import com.ledger.app.domain.model.TaskStatus
import com.ledger.app.domain.model.TransactionDirection
import com.ledger.app.domain.model.TransactionType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Property-based tests for task-to-transaction conversion functionality.
 * 
 * **Feature: offline-ledger-app, Property 19: Task-to-Transaction Linking**
 * 
 * For any task that is converted to a transaction:
 * - The resulting transaction should have linkedTaskId set to the original task ID
 * - The original task should be marked as COMPLETED
 * 
 * **Validates: Requirements 15.3, 15.5**
 */
class TaskToTransactionPropertyTest : FunSpec({

    /**
     * Generator for valid task titles (non-blank strings).
     */
    fun arbTaskTitle(): Arb<String> = Arb.string(5, 50).map { "Task: $it" }

    /**
     * Generator for valid positive amounts (in cents, converted to BigDecimal).
     */
    fun arbPositiveAmount(): Arb<BigDecimal> = Arb.long(1L, 1_000_000_00L).map { cents ->
        BigDecimal(cents).divide(BigDecimal(100))
    }

    /**
     * Generator for transaction directions.
     */
    fun arbDirection(): Arb<TransactionDirection> = 
        Arb.element(TransactionDirection.GAVE, TransactionDirection.RECEIVED)

    /**
     * Generator for transaction types.
     */
    fun arbTransactionType(): Arb<TransactionType> = 
        Arb.element(TransactionType.LOAN, TransactionType.BILL_PAYMENT, TransactionType.RECHARGE, TransactionType.OTHER)

    /**
     * **Feature: offline-ledger-app, Property 19: Task-to-Transaction Linking**
     * 
     * For any task that is converted to a transaction:
     * - The resulting transaction should have linkedTaskId set to the original task ID
     * - The original task should be marked as COMPLETED
     * 
     * **Validates: Requirements 15.3, 15.5**
     */
    test("Property 19 - Converted task is marked as COMPLETED with linkedTransactionId set") {
        checkAll(100, arbTaskTitle(), arbPositiveAmount(), arbDirection(), arbTransactionType()) { title, amount, direction, type ->
            // Setup repositories
            val taskRepo = FakeTaskRepository()
            val transactionRepo = FakeTransactionRepository()
            val useCase = ConvertTaskToTransactionUseCase(taskRepo, transactionRepo)

            // Create a pending task
            val task = Task(
                id = 0,
                title = title,
                description = "Test description",
                priority = TaskPriority.MEDIUM,
                dueDate = LocalDateTime.now().plusDays(1),
                status = TaskStatus.PENDING,
                linkedCounterpartyId = null,
                linkedTransactionId = null,
                createdAt = LocalDateTime.now(),
                completedAt = null,
                checklistItems = emptyList()
            )
            val taskId = taskRepo.insert(task)

            // Convert task to transaction
            val result = useCase(
                taskId = taskId,
                direction = direction,
                type = type,
                amount = amount,
                accountId = 1L,
                categoryId = 1L
            )

            // Verify result is success
            (result is ConvertTaskToTransactionUseCase.Result.Success) shouldBe true
            val successResult = result as ConvertTaskToTransactionUseCase.Result.Success

            // Property 19: Original task should be marked as COMPLETED
            successResult.completedTask.status shouldBe TaskStatus.COMPLETED
            successResult.completedTask.completedAt shouldNotBe null

            // Property 19: Task should have linkedTransactionId set
            successResult.completedTask.linkedTransactionId shouldBe successResult.transactionId

            // Verify the task in repository also has correct status and link
            val taskAfter = taskRepo.getById(taskId)
            taskAfter shouldNotBe null
            taskAfter!!.status shouldBe TaskStatus.COMPLETED
            taskAfter.linkedTransactionId shouldBe successResult.transactionId
        }
    }

    test("Property 19 - Transaction note is pre-filled with task title") {
        checkAll(100, arbTaskTitle(), arbPositiveAmount()) { title, amount ->
            // Setup repositories
            val taskRepo = FakeTaskRepository()
            val transactionRepo = FakeTransactionRepository()
            val useCase = ConvertTaskToTransactionUseCase(taskRepo, transactionRepo)

            // Create a pending task
            val task = Task(
                id = 0,
                title = title,
                description = null,
                priority = TaskPriority.LOW,
                dueDate = null,
                status = TaskStatus.PENDING,
                linkedCounterpartyId = null,
                linkedTransactionId = null,
                createdAt = LocalDateTime.now(),
                completedAt = null,
                checklistItems = emptyList()
            )
            val taskId = taskRepo.insert(task)

            // Convert task to transaction
            val result = useCase(
                taskId = taskId,
                direction = TransactionDirection.GAVE,
                type = TransactionType.LOAN,
                amount = amount,
                accountId = 1L,
                categoryId = 1L
            )

            // Verify result is success
            val successResult = result as ConvertTaskToTransactionUseCase.Result.Success

            // Verify transaction note is pre-filled with task title (Requirements 15.2)
            val transaction = transactionRepo.getById(successResult.transactionId)
            transaction shouldNotBe null
            transaction!!.notes shouldBe title
        }
    }

    test("Property 19 - Transaction counterparty is pre-selected from linked task") {
        checkAll(100, arbTaskTitle(), arbPositiveAmount(), Arb.long(1L, 100L)) { title, amount, counterpartyId ->
            // Setup repositories
            val taskRepo = FakeTaskRepository()
            val transactionRepo = FakeTransactionRepository()
            val useCase = ConvertTaskToTransactionUseCase(taskRepo, transactionRepo)

            // Create a pending task with linked counterparty
            val task = Task(
                id = 0,
                title = title,
                description = null,
                priority = TaskPriority.HIGH,
                dueDate = null,
                status = TaskStatus.PENDING,
                linkedCounterpartyId = counterpartyId,
                linkedTransactionId = null,
                createdAt = LocalDateTime.now(),
                completedAt = null,
                checklistItems = emptyList()
            )
            val taskId = taskRepo.insert(task)

            // Convert task to transaction
            val result = useCase(
                taskId = taskId,
                direction = TransactionDirection.GAVE,
                type = TransactionType.LOAN,
                amount = amount,
                accountId = 1L,
                categoryId = 1L
            )

            // Verify result is success
            val successResult = result as ConvertTaskToTransactionUseCase.Result.Success

            // Verify transaction counterparty is pre-selected (Requirements 15.4)
            val transaction = transactionRepo.getById(successResult.transactionId)
            transaction shouldNotBe null
            transaction!!.counterpartyId shouldBe counterpartyId
        }
    }

    test("Property 19 - Already completed task cannot be converted") {
        checkAll(100, arbTaskTitle(), arbPositiveAmount()) { title, amount ->
            // Setup repositories
            val taskRepo = FakeTaskRepository()
            val transactionRepo = FakeTransactionRepository()
            val useCase = ConvertTaskToTransactionUseCase(taskRepo, transactionRepo)

            // Create an already completed task
            val task = Task(
                id = 0,
                title = title,
                description = null,
                priority = TaskPriority.MEDIUM,
                dueDate = null,
                status = TaskStatus.COMPLETED,
                linkedCounterpartyId = null,
                linkedTransactionId = 999L,
                createdAt = LocalDateTime.now().minusDays(1),
                completedAt = LocalDateTime.now(),
                checklistItems = emptyList()
            )
            val taskId = taskRepo.insert(task)

            // Try to convert the already completed task
            val result = useCase(
                taskId = taskId,
                direction = TransactionDirection.GAVE,
                type = TransactionType.LOAN,
                amount = amount,
                accountId = 1L,
                categoryId = 1L
            )

            // Should return TaskAlreadyCompleted
            result shouldBe ConvertTaskToTransactionUseCase.Result.TaskAlreadyCompleted(taskId)
        }
    }

    test("Property 19 - Cancelled task cannot be converted") {
        checkAll(100, arbTaskTitle(), arbPositiveAmount()) { title, amount ->
            // Setup repositories
            val taskRepo = FakeTaskRepository()
            val transactionRepo = FakeTransactionRepository()
            val useCase = ConvertTaskToTransactionUseCase(taskRepo, transactionRepo)

            // Create a cancelled task
            val task = Task(
                id = 0,
                title = title,
                description = null,
                priority = TaskPriority.LOW,
                dueDate = null,
                status = TaskStatus.CANCELLED,
                linkedCounterpartyId = null,
                linkedTransactionId = null,
                createdAt = LocalDateTime.now(),
                completedAt = null,
                checklistItems = emptyList()
            )
            val taskId = taskRepo.insert(task)

            // Try to convert the cancelled task
            val result = useCase(
                taskId = taskId,
                direction = TransactionDirection.GAVE,
                type = TransactionType.LOAN,
                amount = amount,
                accountId = 1L,
                categoryId = 1L
            )

            // Should return TaskCancelled
            result shouldBe ConvertTaskToTransactionUseCase.Result.TaskCancelled(taskId)
        }
    }

    test("Property 19 - Non-existent task returns TaskNotFound") {
        checkAll(100, Arb.long(1000L, 9999L), arbPositiveAmount()) { nonExistentId, amount ->
            // Setup repositories (empty)
            val taskRepo = FakeTaskRepository()
            val transactionRepo = FakeTransactionRepository()
            val useCase = ConvertTaskToTransactionUseCase(taskRepo, transactionRepo)

            // Try to convert a non-existent task
            val result = useCase(
                taskId = nonExistentId,
                direction = TransactionDirection.GAVE,
                type = TransactionType.LOAN,
                amount = amount,
                accountId = 1L,
                categoryId = 1L
            )

            // Should return TaskNotFound
            result shouldBe ConvertTaskToTransactionUseCase.Result.TaskNotFound(nonExistentId)
        }
    }

    test("Property 19 - Invalid amount returns ValidationError") {
        checkAll(100, arbTaskTitle()) { title ->
            // Setup repositories
            val taskRepo = FakeTaskRepository()
            val transactionRepo = FakeTransactionRepository()
            val useCase = ConvertTaskToTransactionUseCase(taskRepo, transactionRepo)

            // Create a pending task
            val task = Task(
                id = 0,
                title = title,
                description = null,
                priority = TaskPriority.MEDIUM,
                dueDate = null,
                status = TaskStatus.PENDING,
                linkedCounterpartyId = null,
                linkedTransactionId = null,
                createdAt = LocalDateTime.now(),
                completedAt = null,
                checklistItems = emptyList()
            )
            val taskId = taskRepo.insert(task)

            // Try to convert with zero amount
            val result = useCase(
                taskId = taskId,
                direction = TransactionDirection.GAVE,
                type = TransactionType.LOAN,
                amount = BigDecimal.ZERO,
                accountId = 1L,
                categoryId = 1L
            )

            // Should return ValidationError
            (result is ConvertTaskToTransactionUseCase.Result.ValidationError) shouldBe true
        }
    }
})
