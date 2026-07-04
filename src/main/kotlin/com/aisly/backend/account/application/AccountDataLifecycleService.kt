package com.aisly.backend.account.application

import com.aisly.backend.shopping.application.ShoppingWorkspaceService
import com.aisly.backend.shopping.domain.OwnerId
import org.springframework.stereotype.Service

@Service
class AccountDataLifecycleService(
    private val shoppingWorkspaceService: ShoppingWorkspaceService,
) {
    fun archiveUserData(subject: String): AccountLifecycleResult =
        AccountLifecycleResult(shoppingWorkspaceService.archiveAllAccountData(OwnerId(subject)))

    fun deleteUserData(subject: String): AccountLifecycleResult =
        AccountLifecycleResult(shoppingWorkspaceService.deleteAllAccountData(OwnerId(subject)))
}

data class AccountLifecycleResult(val affectedRecords: Int)

