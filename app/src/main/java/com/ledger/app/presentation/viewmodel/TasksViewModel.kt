package com.ledger.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.app.domain.model.ChecklistItem
import com.ledger.app.domain.model.Task
import com.ledger.app.domain.model.TaskPriority
import com.ledger.app.domain.model.TaskStatus
import com.ledger.app.domain.repository.TaskRepository
import com.ledger.app.domain.usecase.AddTaskUseCase
import com.ledger.app.domain.usecase.CompleteTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * ViewModel for the Tasks screen.
 * 
 * Responsibilities:
 * - Expose pending tasks StateFlow
 * - Expose completed tasks StateFlow
 * - Handle sorting (priority, due date)
 * - Handle task completion
 * 
 * Requirements: 14.1
 */
@HiltViewModel
class TasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val addTaskUseCase: AddTaskUseCase,
    private val completeTaskUseCase: CompleteTaskUseCase
) : ViewModel() {

    // Pending tasks
    private val _pendingTasks = MutableStateFlow<List<Task>>(emptyList())
    val pendingTasks: StateFlow<List<Task>> = _pendingTasks.asStateFlow()

    // Completed tasks
    private val _completedTasks = MutableStateFlow<List<Task>>(emptyList())
    val completedTasks: StateFlow<List<Task>> = _completedTasks.asStateFlow()

    // Current sort option
    private val _sortOption = MutableStateFlow(TaskSortOption.PRIORITY)
    val sortOption: StateFlow<TaskSortOption> = _sortOption.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Animation state for task completion
    private val _showCompletionAnimation = MutableStateFlow<Long?>(null)
    val showCompletionAnimation: StateFlow<Long?> = _showCompletionAnimation.asStateFlow()

    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        observePendingTasks()
        observeCompletedTasks()
    }

    /**
     * Observes pending tasks from the repository.
     */
    private fun observePendingTasks() {
        viewModelScope.launch {
            taskRepository.getPendingTasks()
                .catch { e ->
                    _error.value = e.message
                }
                .collect { tasks ->
                    _pendingTasks.value = sortTasks(tasks, _sortOption.value)
                }
        }
    }

    /**
     * Observes completed tasks from the repository.
     */
    private fun observeCompletedTasks() {
        viewModelScope.launch {
            taskRepository.getCompletedTasks()
                .catch { e ->
                    _error.value = e.message
                }
                .collect { tasks ->
                    // Sort completed tasks by completion date (newest first)
                    _completedTasks.value = tasks.sortedByDescending { it.completedAt }
                }
        }
    }

    /**
     * Sorts tasks based on the selected sort option.
     */
    private fun sortTasks(tasks: List<Task>, sortOption: TaskSortOption): List<Task> {
        return when (sortOption) {
            TaskSortOption.PRIORITY -> tasks.sortedWith(
                compareBy<Task> { 
                    when (it.priority) {
                        TaskPriority.HIGH -> 0
                        TaskPriority.MEDIUM -> 1
                        TaskPriority.LOW -> 2
                    }
                }.thenBy { it.dueDate ?: LocalDateTime.MAX }
            )
            TaskSortOption.DUE_DATE -> tasks.sortedWith(
                compareBy<Task> { it.dueDate ?: LocalDateTime.MAX }
                    .thenBy { 
                        when (it.priority) {
                            TaskPriority.HIGH -> 0
                            TaskPriority.MEDIUM -> 1
                            TaskPriority.LOW -> 2
                        }
                    }
            )
            TaskSortOption.CREATED_DATE -> tasks.sortedByDescending { it.createdAt }
        }
    }

    /**
     * Updates the sort option and re-sorts pending tasks.
     */
    fun setSortOption(option: TaskSortOption) {
        _sortOption.value = option
        _pendingTasks.value = sortTasks(_pendingTasks.value, option)
    }

    /**
     * Completes a task and triggers the completion animation.
     * 
     * Requirements: 14.4
     */
    fun completeTask(task: Task) {
        viewModelScope.launch {
            when (val result = completeTaskUseCase(task.id)) {
                is CompleteTaskUseCase.Result.Success -> {
                    if (result.shouldShowAnimation) {
                        _showCompletionAnimation.value = task.id
                    }
                }
                is CompleteTaskUseCase.Result.TaskNotFound -> {
                    _error.value = "Task not found"
                }
                is CompleteTaskUseCase.Result.AlreadyCompleted -> {
                    // Task already completed, no action needed
                }
                is CompleteTaskUseCase.Result.TaskCancelled -> {
                    _error.value = "Cannot complete a cancelled task"
                }
            }
        }
    }

    /**
     * Clears the completion animation state.
     */
    fun clearCompletionAnimation() {
        _showCompletionAnimation.value = null
    }

    /**
     * Deletes a task.
     */
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            try {
                taskRepository.delete(task.id)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    /**
     * Clears the error state.
     */
    fun clearError() {
        _error.value = null
    }
}

/**
 * Sort options for tasks.
 */
enum class TaskSortOption {
    PRIORITY,
    DUE_DATE,
    CREATED_DATE
}
