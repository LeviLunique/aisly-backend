package com.aisly.backend.shopping

import com.aisly.backend.shopping.domain.OwnerId
import com.aisly.backend.shopping.domain.ShoppingList
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class ShoppingDomainTest {
    @Test
    fun `template lists are derived from recurrence`() {
        val list = ShoppingList.create(OwnerId("user-1"), UUID.randomUUID(), "Compra", Instant.parse("2026-01-01T00:00:00Z"))

        assertFalse(list.template)
        assertTrue(list.categories.contains("Others"))
    }
}

