package com.ledger.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a checklist item within a task.
 * Uses cascade delete to automatically remove items when parent task is deleted.
 */
@Entity(
    tableName = "checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("taskId"),
        Index("sortOrder")
    ]
)
data class ChecklistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long, // Foreign key to parent task
    val text: String,
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0 // For maintaining item order within a task
)
