package com.aisly.backend

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class HistoryApiTests : ApiTestSupport() {

    private fun finishAList(owner: String): String {
        val listId = mockMvc.perform(
            post("/api/v1/lists").with(user(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Shop","items":[{"name":"Milk","categoryName":"Dairy","completed":true,"plannedPrice":4.00}]}"""),
        ).andReturn().json().get("id").asString()
        return mockMvc.perform(post("/api/v1/lists/$listId/finish").with(user(owner)))
            .andExpect(status().isCreated)
            .andReturn().json().get("id").asString()
    }

    @Test
    fun `history entries are isolated per owner`() {
        val ownerA = newOwner()
        val ownerB = newOwner()
        val historyId = finishAList(ownerA)

        mockMvc.perform(get("/api/v1/history/$historyId").with(user(ownerA)))
            .andExpect(status().isOk)
        mockMvc.perform(get("/api/v1/history/$historyId").with(user(ownerB)))
            .andExpect(status().isNotFound)
        mockMvc.perform(post("/api/v1/history/$historyId/repeat").with(user(ownerB)))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `history can be repeated and turned into a template`() {
        val owner = newOwner()
        val historyId = finishAList(owner)

        mockMvc.perform(post("/api/v1/history/$historyId/repeat").with(user(owner)))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.templateRecurrence").value(null))
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].name").value("Milk"))
            .andExpect(jsonPath("$.items[0].completed").value(false))

        mockMvc.perform(post("/api/v1/history/$historyId/template").with(user(owner)))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.templateRecurrence").value("weekly"))
            .andExpect(jsonPath("$.items[0].name").value("Milk"))
    }

    @Test
    fun `history entries can be deleted`() {
        val owner = newOwner()
        val historyId = finishAList(owner)
        mockMvc.perform(delete("/api/v1/history/$historyId").with(user(owner)))
            .andExpect(status().isNoContent)
        mockMvc.perform(get("/api/v1/history/$historyId").with(user(owner)))
            .andExpect(status().isNotFound)
    }
}
