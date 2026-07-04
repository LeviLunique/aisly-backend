package com.aisly.backend.shopping.domain

import java.time.Instant
import java.util.UUID

sealed interface ShoppingDomainEvent {
    val ownerId: OwnerId
    val occurredAt: Instant
}

data class ListCreatedEvent(
    override val ownerId: OwnerId,
    val listId: UUID,
    override val occurredAt: Instant,
) : ShoppingDomainEvent

data class TemplateUsedEvent(
    override val ownerId: OwnerId,
    val templateId: UUID,
    val generatedListId: UUID,
    override val occurredAt: Instant,
) : ShoppingDomainEvent

data class PurchaseFinishedEvent(
    override val ownerId: OwnerId,
    val listId: UUID,
    val historyId: UUID,
    override val occurredAt: Instant,
) : ShoppingDomainEvent

data class AccountDataArchivedEvent(
    override val ownerId: OwnerId,
    val archivedListCount: Int,
    override val occurredAt: Instant,
) : ShoppingDomainEvent

