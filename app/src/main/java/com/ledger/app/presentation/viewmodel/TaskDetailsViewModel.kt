package com.ledger.app.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.app.domain.model.ChecklistItem
import com.ledger.app.domain.model.Counterparty
import com.ledger.app.domain.model.Task
import com.ledger.app.domain.model.TaskStatus
import com.ledger.app.domain.repository.CounterpartyRepository
import com.ledger.app.domain.repository.TaskRepository
import com.ledger.app.domain.usecase.CompleteTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Task Details screen.
 * 
 * Handles displaying task details, checklist management,
 * task completion, and conversion to transaction.
 * 
 * Requirements: 14.3, 15.1, 15.5
 */
@HiltViewModel
class TaskDetailsViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val counterpartyRepository: CounterpartyRepository,
    private val completeTaskUseCase: CompleteTaskUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val taskId: Long = savedStateHandle.get<Long>("taskId") ?: 0L
    
    // Task state
    private val _task = MutableStateFlow<Task?>(null)
    val task: StateFlow<Task?> = _task.asStateFlow()
    
    // Linked counterparty
    private val _linkedCounterparty = MutableStateFlow<Counterparty?>(null)
    val linkedCounterparty: StateFlow<Counterparty?> = _linkedCounterparty.asStateFlow()
    
    // UI state
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _showCompletionAnimation = MutableStateFlow(false)
    val showCompletionAnimation: StateFlow<Boolean> = _showCompletionAnimation.asStateFlow()
    
    private val _taskDeleted = MutableStateFlow(false)
    val taskDeleted: StateFlow<Boolean> = _taskDeleted.asStateFlow()
    
    init {
        loadTask()
    }
    
    /**
     * Loads the task details.
     */
    private fun loadTask() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val loadedTask = taskRepository.getById(taskId)
                _task.value = loadedTask
                
                // Load linked counterparty if exists
                loadedTask?.linkedCounterpartyId?.let { counterpartyId ->
                    _linkedCounterparty.value = counterpartyRepository.getById(counterpartyId)
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Toggles a checklist item's completion status.
     */
    fun toggleChecklistItem(item: ChecklistItem) {
        viewModelScope.launch {
            try {
                taskRepository.setChecklistItemCompleted(item.id, !item.isCompleted)
                // Reload task to get updated checklist
                loadTask()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
    
    /**
     * Completes the task.
     * 
     * Requirements: 14.4
     */
    fun completeTask() {
        viewModelScope.launch {
            when (val result = completeTaskUseCase(taskId)) {
                is CompleteTaskUseCase.Result.Success -> {
                    _showCompletionAnimation.value = true
                    _task.value = result.task
                }
                is CompleteTaskUseCase.Result.TaskNotFound -> {
                    _error.value = "Task not found"
                }
                is CompleteTaskUseCase.Result.AlreadyCompleted -> {
                    _error.value = "Task is already completed"
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
        _showCompletionAnimation.value = false
    }
    
    /**
     * Deletes the task.
     */
    fun deleteTask() {
        viewModelScope.launch {
            try {
                taskRepository.delete(taskId)
                _taskDeleted.value = true
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
    
    /**
     * Checks if the task can be converted to a transaction.
     * A task can be converted if it's pending and not already linked to a transaction.
     */
    fun canConvertToTransaction(): Boolean {
        val currentTask = _task.value ?: return false
        return currentTask.status == TaskStatus.PENDING && 
               currentTask.linkedTransactionId == null
    }
}
