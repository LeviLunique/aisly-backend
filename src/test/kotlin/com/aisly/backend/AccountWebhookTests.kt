package com.aisly.backend

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class AccountWebhookTests : ApiTestSupport() {

    private fun createList(owner: String, name: String) =
        mockMvc.perform(
            post("/api/v1/lists").with(user(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"$name"}"""),
        ).andExpect(status().isCreated)

    @Test
    fun `rejects a missing or wrong internal secret`() {
        val owner = newOwner()
        mockMvc.perform(post("/internal/v1/users/$owner/delete-data"))
            .andExpect(status().isForbidden)
        mockMvc.perform(post("/internal/v1/users/$owner/delete-data").header("X-Aisly-Internal-Secret", "wrong"))
            .andExpect(status().isForbidden)
        mockMvc.perform(post("/internal/v1/users/$owner/archive-data").header("X-Aisly-Internal-Secret", "wrong"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `archive-data archives every list of the user`() {
        val owner = newOwner()
        createList(owner, "One")
        createList(owner, "Two")

        mockMvc.perform(post("/internal/v1/users/$owner/archive-data").header("X-Aisly-Internal-Secret", "test-secret"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.affectedRecords").value(2))

        mockMvc.perform(get("/api/v1/lists").with(user(owner)))
            .andExpect(jsonPath("$.length()").value(0))
        mockMvc.perform(get("/api/v1/lists?includeArchived=true").with(user(owner)))
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    fun `delete-data wipes lists, categories, catalog and history`() {
        val owner = newOwner()
        createList(owner, "One") // also seeds the 7 default categories

        mockMvc.perform(post("/internal/v1/users/$owner/delete-data").header("X-Aisly-Internal-Secret", "test-secret"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.affectedRecords").value(8))

        mockMvc.perform(get("/api/v1/lists?includeArchived=true").with(user(owner)))
            .andExpect(jsonPath("$.length()").value(0))
        mockMvc.perform(get("/api/v1/history").with(user(owner)))
            .andExpect(jsonPath("$.length()").value(0))
    }
}
