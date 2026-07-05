package com.aisly.backend.account

import com.aisly.backend.account.responses.AccountLifecycleResult
import com.aisly.backend.catalog.CatalogRepository
import com.aisly.backend.categories.CategoryRepository
import com.aisly.backend.history.HistoryRepository
import com.aisly.backend.lists.ListRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Data lifecycle operations invoked by the AuthServer webhook. */
@Service
class AccountService(
    private val lists: ListRepository,
    private val categories: CategoryRepository,
    private val catalog: CatalogRepository,
    private val history: HistoryRepository,
) {
    @Transactional
    fun archiveUserData(subject: String): AccountLifecycleResult {
        val owned = lists.findByOwnerId(subject)
        owned.forEach { it.archived = true }
        lists.saveAll(owned)
        return AccountLifecycleResult(owned.size)
    }

    @Transactional
    fun deleteUserData(subject: String): AccountLifecycleResult {
        val listCount = lists.countByOwnerId(subject).toInt()
        val deleted = history.deleteByOwnerId(subject).toInt() +
            catalog.deleteByOwnerId(subject).toInt() +
            categories.deleteByOwnerId(subject).toInt()
        lists.deleteByOwnerId(subject)
        return AccountLifecycleResult(deleted + listCount)
    }
}
