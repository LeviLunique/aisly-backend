package com.aisly.backend

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

class ListApiTests : ApiTestSupport() {

    private fun createList(owner: String, body: String) =
        mockMvc.perform(post("/api/v1/lists").with(user(owner)).contentType(MediaType.APPLICATION_JSON).content(body))

    @Test
    fun `requires a JWT on every api endpoint`() {
        mockMvc.perform(get("/api/v1/lists")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `creates a list with inline items, computed totals and default categories`() {
        val owner = newOwner()
        createList(
            owner,
            """
            {"name":"Groceries","budget":100,
             "items":[
               {"name":"Milk","categoryName":"Dairy","quantity":2,"unit":"L","plannedPrice":4.50},
               {"name":"Soap","categoryName":"Household","completed":true,"plannedPrice":3.00,"actualPrice":2.50}
             ]}
            """.trimIndent(),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("Groceries"))
            .andExpect(jsonPath("$.archived").value(false))
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.items[0].name").value("Milk"))
            .andExpect(jsonPath("$.items[0].unit").value("L"))
            .andExpect(jsonPath("$.plannedTotal").value(12.0))
            .andExpect(jsonPath("$.actualTotal").value(2.5))
            .andExpect(jsonPath("$.purchasedItemCount").value(1))
            .andExpect(jsonPath("$.missingActualPriceCount").value(0))
            .andExpect(jsonPath("$.categories[?(@=='Other')]").exists())
            .andExpect(jsonPath("$.categories[?(@=='Dairy')]").exists())

        // Creating the first list also seeds the user's default categories.
        mockMvc.perform(get("/api/v1/categories").with(user(owner)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(7))
    }

    @Test
    fun `upsert by client id makes create retries idempotent`() {
        val owner = newOwner()
        val listId = UUID.randomUUID()
        val itemId = UUID.randomUUID()
        val body = """
            {"id":"$listId","name":"Sync","items":[{"id":"$itemId","name":"Bread","categoryName":"Pantry"}]}
        """.trimIndent()

        createList(owner, body)
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(listId.toString()))
            .andExpect(jsonPath("$.items[0].id").value(itemId.toString()))

        // Retrying the exact same request must not duplicate anything.
        createList(owner, body)
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(listId.toString()))
            .andExpect(jsonPath("$.items.length()").value(1))

        mockMvc.perform(get("/api/v1/lists").with(user(owner)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    fun `an id owned by another user is reported as 404 and never leaks`() {
        val ownerA = newOwner()
        val ownerB = newOwner()
        val listId = UUID.randomUUID()
        createList(ownerA, """{"id":"$listId","name":"Mine"}""").andExpect(status().isCreated)

        mockMvc.perform(get("/api/v1/lists/$listId").with(user(ownerB)))
            .andExpect(status().isNotFound)
        createList(ownerB, """{"id":"$listId","name":"Steal"}""")
            .andExpect(status().isNotFound)
        mockMvc.perform(delete("/api/v1/lists/$listId").with(user(ownerB)))
            .andExpect(status().isNotFound)

        // And the real owner still sees the original list untouched.
        mockMvc.perform(get("/api/v1/lists/$listId").with(user(ownerA)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Mine"))
    }

    @Test
    fun `full item lifecycle inside a list`() {
        val owner = newOwner()
        val listId = createList(owner, """{"name":"Market"}""").andReturn().json().get("id").asString()

        val withItem = mockMvc.perform(
            post("/api/v1/lists/$listId/items").with(user(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Rice","categoryName":"Pantry","quantity":2,"plannedPrice":5.00}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.items.length()").value(1))
            .andReturn().json()
        val itemId = withItem.get("items").get(0).get("id").asString()

        mockMvc.perform(
            put("/api/v1/lists/$listId/items/$itemId").with(user(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Brown Rice","categoryName":"Pantry","quantity":1,"actualPrice":6.00}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].name").value("Brown Rice"))
            .andExpect(jsonPath("$.actualTotal").value(6.0))

        mockMvc.perform(
            patch("/api/v1/lists/$listId/items/$itemId/completion").with(user(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"completed":true}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.purchasedItemCount").value(1))

        mockMvc.perform(delete("/api/v1/lists/$listId/items/$itemId").with(user(owner)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(0))
    }

    @Test
    fun `lists are ordered by pinned, manual order and recency`() {
        val owner = newOwner()
        val first = createList(owner, """{"name":"First"}""").andReturn().json().get("id").asString()
        val second = createList(owner, """{"name":"Second"}""").andReturn().json().get("id").asString()

        mockMvc.perform(
            put("/api/v1/lists/reorder").with(user(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"ids":["$second","$first"]}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(second))
            .andExpect(jsonPath("$[1].id").value(first))

        // Reorder must include every active list.
        mockMvc.perform(
            put("/api/v1/lists/reorder").with(user(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"ids":["$first"]}"""),
        )
            .andExpect(status().isBadRequest)

        // Pinned lists jump ahead of the manual order.
        mockMvc.perform(
            patch("/api/v1/lists/$first/pin").with(user(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"pinned":true}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.pinned").value(true))
        mockMvc.perform(get("/api/v1/lists").with(user(owner)))
            .andExpect(jsonPath("$[0].id").value(first))
    }

    @Test
    fun `archived lists disappear unless explicitly requested`() {
        val owner = newOwner()
        val listId = createList(owner, """{"name":"Old"}""").andReturn().json().get("id").asString()

        mockMvc.perform(
            patch("/api/v1/lists/$listId/archive").with(user(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"archived":true}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.archived").value(true))

        mockMvc.perform(get("/api/v1/lists").with(user(owner)))
            .andExpect(jsonPath("$.length()").value(0))
        mockMvc.perform(get("/api/v1/lists?includeArchived=true").with(user(owner)))
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    fun `finishing a purchase snapshots history, archives the list and is idempotent`() {
        val owner = newOwner()
        val listId = createList(
            owner,
            """
            {"name":"Weekly shop","budget":50,
             "items":[
               {"name":"Milk","categoryName":"Dairy","completed":true,"plannedPrice":4.00,"actualPrice":4.50},
               {"name":"Soap","categoryName":"Household","plannedPrice":3.00}
             ]}
            """.trimIndent(),
        ).andReturn().json().get("id").asString()

        val snapshotId = UUID.randomUUID()
        val finishBody = """{"id":"$snapshotId","finishedAt":"2026-01-02T10:00:00Z"}"""

        mockMvc.perform(
            post("/api/v1/lists/$listId/finish").with(user(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content(finishBody),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(snapshotId.toString()))
            .andExpect(jsonPath("$.sourceListId").value(listId))
            .andExpect(jsonPath("$.finishedAt").value("2026-01-02T10:00:00Z"))
            .andExpect(jsonPath("$.purchasedItemCount").value(1))
            .andExpect(jsonPath("$.totalItemCount").value(2))
            .andExpect(jsonPath("$.sections.length()").value(1))
            .andExpect(jsonPath("$.sections[0].name").value("Dairy"))
            .andExpect(jsonPath("$.sections[0].items[0].name").value("Milk"))

        // The source list was archived by the finish.
        mockMvc.perform(get("/api/v1/lists/$listId").with(user(owner)))
            .andExpect(jsonPath("$.archived").value(true))

        // Retrying with the same snapshot id returns the same entry and creates nothing new.
        mockMvc.perform(
            post("/api/v1/lists/$listId/finish").with(user(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content(finishBody),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(snapshotId.toString()))

        mockMvc.perform(get("/api/v1/history").with(user(owner)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(snapshotId.toString()))
    }

    @Test
    fun `deleting a list removes it`() {
        val owner = newOwner()
        val listId = createList(owner, """{"name":"Temp"}""").andReturn().json().get("id").asString()
        mockMvc.perform(delete("/api/v1/lists/$listId").with(user(owner))).andExpect(status().isNoContent)
        mockMvc.perform(get("/api/v1/lists/$listId").with(user(owner))).andExpect(status().isNotFound)
    }
}
