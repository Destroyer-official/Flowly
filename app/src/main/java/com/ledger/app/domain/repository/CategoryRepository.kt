package com.ledger.app.domain.repository

import com.ledger.app.domain.model.Category
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for category data access.
 */
interface CategoryRepository {
    suspend fun insert(category: Category): Long
    suspend fun getById(id: Long): Category?
    fun getAll(): Flow<List<Category>>
}
