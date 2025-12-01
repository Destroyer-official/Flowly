package com.ledger.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconName: String,
    val colorKey: String,
    val isBillCategory: Boolean = false // For Electricity, TV, Mobile, Internet categories
)
