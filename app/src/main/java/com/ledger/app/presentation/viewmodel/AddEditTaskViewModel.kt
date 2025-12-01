package com.ledger.app.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.app.domain.model.ChecklistItem
import com.ledger.app.domain.model.Counterparty
import com.ledger.app.domain.model.Task
import com.ledger.app.domain.model.TaskPriority
import com.ledger.app.domain.repository.CounterpartyRepository
import com.ledger.app.domain.repository.TaskRepository
import com.ledger.app.domain.usecase.AddTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * ViewModel for Add/Edit Task screen.
 * 
 * Handles task creation and editing with validation.
 * 
 * Requirements: 14.2, 14.3
 */
@HiltViewModel
class AddEditTaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val counterpartyRepository: CounterpartyRepository,
    private val addTaskUseCase: AddTaskUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Task ID for editing (null for new task)
    private val taskId: Long? = savedStateHandle.get<Long>("taskId")?.takeIf { it > 0 }
    
    // Form state
    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()
    
    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()
    
    private val _priority = MutableStateFlow(TaskPriority.MEDIUM)
    val priority: StateFlow<TaskPriority> = _priority.asStateFlow()
    
    private val _dueDate = MutableStateFlow<LocalDateTime?>(null)
    val dueDate: StateFlow<LocalDateTime?> = _dueDate.asStateFlow()
    
    private val _linkedCounterpartyId = MutableStateFlow<Long?>(null)
    val linkedCounterpartyId: StateFlow<Long?> = _linkedCounterpartyId.asStateFlow()
    
    private val _checklistItems = MutableStateFlow<List<ChecklistItem>>(emptyList())
    val checklistItems: StateFlow<List<ChecklistItem>> = _checklistItems.asStateFlow()
    
    // Available counterparties for selection
    private val _counterparties = MutableStateFlow<List<Counterparty>>(emptyList())
    val counterparties: StateFlow<List<Counterparty>> = _counterparties.asStateFlow()
    
    // UI state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()
    
    // Edit mode flag
    val isEditMode: Boolean = taskId != null
    
    init {
        loadCounterparties()
        if (taskId != null) {
            loadTask(taskId)
        }
    }
    
    /**
     * Loads counterparties for the selector.
     */
    private fun loadCounterparties() {
        viewModelScope.launch {
            counterpartyRepository.getAll()
                .catch { e ->
                    _error.value = e.message
                }
                .collect { list ->
                    _counterparties.value = list
                }
        }
    }
    
    /**
     * Loads an existing task for editing.
     */
    private fun loadTask(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val task = taskRepository.getById(id)
                if (task != null) {
                    _title.value = task.title
                    _description.value = task.description ?: ""
                    _priority.value = task.priority
                    _dueDate.value = task.dueDate
                    _linkedCounterpartyId.value = task.linkedCounterpartyId
                    _checklistItems.value = task.checklistItems
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Form update functions
    fun updateTitle(value: String) {
        _title.value = value
    }
    
    fun updateDescription(value: String) {
        _description.value = value
    }
    
    fun updatePriority(value: TaskPriority) {
        _priority.value = value
    }
    
    fun updateDueDate(value: LocalDateTime?) {
        _dueDate.value = value
    }
    
    fun updateLinkedCounterparty(counterpartyId: Long?) {
        _linkedCounterpartyId.value = counterpartyId
    }
    
    fun updateChecklistItems(items: List<ChecklistItem>) {
        _checklistItems.value = items
    }
    
    /**
     * Validates the form and returns true if valid.
     */
    fun isFormValid(): Boolean {
        return _title.value.trim().isNotBlank()
    }
    
    /**
     * Saves the task (creates new or updates existing).
     */
    fun saveTask() {
        if (!isFormValid()) {
            _error.value = "Task title is required"
            return
        }
        
        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null
            
            try {
                if (isEditMode && taskId != null) {
                    // Update existing task
                    val existingTask = taskRepository.getById(taskId)
                    if (existingTask != null) {
                        val updatedTask = existingTask.copy(
                            title = _title.value.trim(),
                            description = _description.value.trim().takeIf { it.isNotBlank() },
                            priority = _priority.value,
                            dueDate = _dueDate.value,
                            linkedCounterpartyId = _linkedCounterpartyId.value,
                            checklistItems = _checklistItems.value
                        )
                        taskRepository.update(updatedTask)
                        
                        // Update checklist items
                        // First, delete existing items
                        existingTask.checklistItems.forEach { item ->
                            taskRepository.deleteChecklistItem(item.id)
                        }
                        // Then add new items
                        _checklistItems.value.forEach { item ->
                            taskRepository.addChecklistItem(item.copy(taskId = taskId))
                        }
                        
                        _saveSuccess.value = true
                    } else {
                        _error.value = "Task not found"
                    }
                } else {
                    // Create new task
                    val result = addTaskUseCase(
                        title = _title.value.trim(),
                        description = _description.value.trim().takeIf { it.isNotBlank() },
                        priority = _priority.value,
                        dueDate = _dueDate.value,
                        linkedCounterpartyId = _linkedCounterpartyId.value,
                        checklistItems = _checklistItems.value
                    )
                    
                    when (result) {
                        is AddTaskUseCase.Result.Success -> {
                            _saveSuccess.value = true
                        }
                        is AddTaskUseCase.Result.ValidationError -> {
                            _error.value = result.message
                        }
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to save task"
            } finally {
                _isSaving.value = false
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
