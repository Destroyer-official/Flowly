package com.ledger.app.domain.usecase

import com.ledger.app.domain.model.Task
import com.ledger.app.domain.model.TaskPriority
import com.ledger.app.domain.model.TaskStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime

/**
 * Property-based tests for task completion functionality.
 * 
 * **Feature: offline-ledger-app, Property 18: Task Completion Status**
 * 
 * For any task that is marked as completed:
 * - The task status should be COMPLETED
 * - The completedAt timestamp should be set
 * - The task should appear in the completed tasks list (not pending)
 * 
 * **Validates: Requirements 14.4**
 */
class TaskCompletionPropertyTest : FunSpec({

    /**
     * Generator for valid task titles (non-blank strings).
     */
    fun arbTaskTitle(): Arb<String> = Arb.string(5, 50).map { "Task: $it" }

    /**
     * Generator for task priorities.
     */
    fun arbPriority(): Arb<TaskPriority> = 
        Arb.element(TaskPriority.HIGH, TaskPriority.MEDIUM, TaskPriority.LOW)

    /**
     * **Feature: offline-ledger-app, Property 18: Task Completion Status**
     * 
     * For any task that is marked as completed:
     * - The task status should be COMPLETED
     * - The completedAt timestamp should be set
     * - The task should appear in the completed tasks list (not pending)
     * 
     * **Validates: Requirements 14.4**
     */
    test("Property 18 - Completed task has COMPLETED status and completedAt timestamp set") {
        checkAll(100, arbTaskTitle(), arbPriority()) { title, priority ->
            // Setup repository
            val taskRepo = FakeTaskRepository()
            val useCase = CompleteTaskUseCase(taskRepo)

            // Create a pending task
            val task = Task(
                id = 0,
                title = title,
                description = "Test description",
                priority = priority,
                dueDate = LocalDateTime.now().plusDays(1),
                status = TaskStatus.PENDING,
                linkedCounterpartyId = null,
                linkedTransactionId = null,
                createdAt = LocalDateTime.now(),
                completedAt = null,
                checklistItems = emptyList()
            )
            val taskId = taskRepo.insert(task)

            // Complete the task
            val result = useCase(taskId)

            // Verify result is success
            (result is CompleteTaskUseCase.Result.Success) shouldBe true
            val successResult = result as CompleteTaskUseCase.Result.Success

            // Property 18: Task status should be COMPLETED
            successResult.task.status shouldBe TaskStatus.COMPLETED

            // Property 18: completedAt timestamp should be set
            successResult.task.completedAt shouldNotBe null

            // Verify the task in repository also has correct status
            val taskAfter = taskRepo.getById(taskId)
            taskAfter shouldNotBe null
            taskAfter!!.status shouldBe TaskStatus.COMPLETED
            taskAfter.completedAt shouldNotBe null
        }
    }

    /**
     * **Feature: offline-ledger-app, Property 18: Task Completion Status**
     * 
     * For any task that is marked as completed, the task should appear in the 
     * completed tasks list (not pending).
     * 
     * **Validates: Requirements 14.4**
     */
    test("Property 18 - Completed task appears in completed list, not pending list") {
        checkAll(100, arbTaskTitle(), arbPriority()) { title, priority ->
            // Setup repository
            val taskRepo = FakeTaskRepository()
            val useCase = CompleteTaskUseCase(taskRepo)

            // Create a pending task
            val task = Task(
                id = 0,
                title = title,
                description = null,
                priority = priority,
                dueDate = null,
                status = TaskStatus.PENDING,
                linkedCounterpartyId = null,
                linkedTransactionId = null,
                createdAt = LocalDateTime.now(),
                completedAt = null,
                checklistItems = emptyList()
            )
            val taskId = taskRepo.insert(task)

            // Verify task is initially in pending list
            val pendingBefore = taskRepo.getPendingTasks().first()
            pendingBefore.any { it.id == taskId } shouldBe true

            val completedBefore = taskRepo.getCompletedTasks().first()
            completedBefore.any { it.id == taskId } shouldBe false

            // Complete the task
            val result = useCase(taskId)
            (result is CompleteTaskUseCase.Result.Success) shouldBe true

            // Property 18: Task should appear in completed list, not pending
            val pendingAfter = taskRepo.getPendingTasks().first()
            pendingAfter.any { it.id == taskId } shouldBe false

            val completedAfter = taskRepo.getCompletedTasks().first()
            completedAfter.any { it.id == taskId } shouldBe true
        }
    }

    /**
     * **Feature: offline-ledger-app, Property 18: Task Completion Status**
     * 
     * For any task that is marked as completed, the shouldShowAnimation flag 
     * should be true (satisfying micro-interaction animation).
     * 
     * **Validates: Requirements 14.4**
     */
    test("Property 18 - Completed task triggers animation flag") {
        checkAll(100, arbTaskTitle(), arbPriority()) { title, priority ->
            // Setup repository
            val taskRepo = FakeTaskRepository()
            val useCase = CompleteTaskUseCase(taskRepo)

            // Create a pending task
            val task = Task(
                id = 0,
                title = title,
                description = "Animation test",
                priority = priority,
                dueDate = LocalDateTime.now().plusDays(7),
                status = TaskStatus.PENDING,
                linkedCounterpartyId = null,
                linkedTransactionId = null,
                createdAt = LocalDateTime.now(),
                completedAt = null,
                checklistItems = emptyList()
            )
            val taskId = taskRepo.insert(task)

            // Complete the task
            val result = useCase(taskId)

            // Verify result is success with animation flag
            (result is CompleteTaskUseCase.Result.Success) shouldBe true
            val successResult = result as CompleteTaskUseCase.Result.Success

            // Property 18: Animation flag should be true for satisfying micro-interaction
            successResult.shouldShowAnimation shouldBe true
        }
    }

    /**
     * Test that already completed tasks return AlreadyCompleted result.
     */
    test("Property 18 - Already completed task returns AlreadyCompleted") {
        checkAll(100, arbTaskTitle()) { title ->
            // Setup repository
            val taskRepo = FakeTaskRepository()
            val useCase = CompleteTaskUseCase(taskRepo)

            // Create an already completed task
            val task = Task(
                id = 0,
                title = title,
                description = null,
                priority = TaskPriority.MEDIUM,
                dueDate = null,
                status = TaskStatus.COMPLETED,
                linkedCounterpartyId = null,
                linkedTransactionId = null,
                createdAt = LocalDateTime.now().minusDays(1),
                completedAt = LocalDateTime.now(),
                checklistItems = emptyList()
            )
            val taskId = taskRepo.insert(task)

            // Try to complete the already completed task
            val result = useCase(taskId)

            // Should return AlreadyCompleted
            result shouldBe CompleteTaskUseCase.Result.AlreadyCompleted(taskId)
        }
    }

    /**
     * Test that cancelled tasks return TaskCancelled result.
     */
    test("Property 18 - Cancelled task returns TaskCancelled") {
        checkAll(100, arbTaskTitle()) { title ->
            // Setup repository
            val taskRepo = FakeTaskRepository()
            val useCase = CompleteTaskUseCase(taskRepo)

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

            // Try to complete the cancelled task
            val result = useCase(taskId)

            // Should return TaskCancelled
            result shouldBe CompleteTaskUseCase.Result.TaskCancelled(taskId)
        }
    }

    /**
     * Test that non-existent task returns TaskNotFound result.
     */
    test("Property 18 - Non-existent task returns TaskNotFound") {
        checkAll(100, Arb.element(1000L, 2000L, 3000L, 9999L)) { nonExistentId ->
            // Setup repository (empty)
            val taskRepo = FakeTaskRepository()
            val useCase = CompleteTaskUseCase(taskRepo)

            // Try to complete a non-existent task
            val result = useCase(nonExistentId)

            // Should return TaskNotFound
            result shouldBe CompleteTaskUseCase.Result.TaskNotFound(nonExistentId)
        }
    }

    /**
     * Test that completedAt timestamp is set to a reasonable time (not in the future).
     */
    test("Property 18 - completedAt timestamp is set to current time (not future)") {
        checkAll(100, arbTaskTitle()) { title ->
            // Setup repository
            val taskRepo = FakeTaskRepository()
            val useCase = CompleteTaskUseCase(taskRepo)

            // Record time before completion
            val timeBefore = LocalDateTime.now()

            // Create a pending task
            val task = Task(
                id = 0,
                title = title,
                description = null,
                priority = TaskPriority.HIGH,
                dueDate = null,
                status = TaskStatus.PENDING,
                linkedCounterpartyId = null,
                linkedTransactionId = null,
                createdAt = LocalDateTime.now().minusHours(1),
                completedAt = null,
                checklistItems = emptyList()
            )
            val taskId = taskRepo.insert(task)

            // Complete the task
            val result = useCase(taskId)
            val timeAfter = LocalDateTime.now()

            // Verify result is success
            (result is CompleteTaskUseCase.Result.Success) shouldBe true
            val successResult = result as CompleteTaskUseCase.Result.Success

            // Property 18: completedAt should be between timeBefore and timeAfter
            val completedAt = successResult.task.completedAt!!
            (completedAt.isAfter(timeBefore.minusSeconds(1)) || completedAt.isEqual(timeBefore)) shouldBe true
            (completedAt.isBefore(timeAfter.plusSeconds(1)) || completedAt.isEqual(timeAfter)) shouldBe true
        }
    }
})
