package com.ledger.app.data.repository

import com.ledger.app.data.local.dao.AccountDao
import com.ledger.app.data.local.entity.AccountEntity
import com.ledger.app.domain.model.Account
import com.ledger.app.domain.model.AccountType
import com.ledger.app.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao
) : AccountRepository {

    override suspend fun insert(account: Account): Long {
        return accountDao.insert(account.toEntity())
    }

    override suspend fun update(account: Account) {
        accountDao.update(account.toEntity())
    }

    override suspend fun deactivate(accountId: Long) {
        accountDao.deactivate(accountId)
    }

    override fun getActive(): Flow<List<Account>> {
        return accountDao.getActive().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getById(id: Long): Account? {
        return accountDao.getById(id)?.toDomain()
    }

    private fun Account.toEntity(): AccountEntity {
        return AccountEntity(
            id = id,
            name = name,
            type = type.name,
            isActive = isActive
        )
    }

    private fun AccountEntity.toDomain(): Account {
        return Account(
            id = id,
            name = name,
            type = AccountType.valueOf(type),
            isActive = isActive
        )
    }
}
