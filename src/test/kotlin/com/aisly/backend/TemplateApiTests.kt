package com.aisly.backend

import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

class TemplateApiTests : ApiTestSupport() {

    @Test
    fun `creates a template with inline items and lists it apart from regular lists`() {
        val owner = newOwner()
        mockMvc.perform(
            post("/api/v1/templates").with(user(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"Weekly","recurrence":"weekly",
                     "items":[{"name":"Milk","categoryName":"Dairy","plannedPrice":4.00}]}
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.templateRecurrence").value("weekly"))
            .andExpect(jsonPath("$.items.length()").value(1))

        mockMvc.perform(get("/api/v1/templates").with(user(owner)))
            .andExpect(jsonPath("$.length()").value(1))
        mockMvc.perform(get("/api/v1/lists").with(user(owner)))
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `using a template instantiates a fresh list linked to it`() {
        val owner = newOwner()
        val templateId = mockMvc.perform(
            post("/api/v1/templates").with(user(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"Monthly","recurrence":"monthly",
                     "items":[{"name":"Rice","categoryName":"Pantry","completed":true,"plannedPrice":5.00,"actualPrice":5.50}]}
                    """.trimIndent(),
                ),
        ).andReturn().json().get("id").asString()

        val list = mockMvc.perform(post("/api/v1/templates/$templateId/use").with(user(owner)))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.templateRecurrence").value(null))
            .andExpect(jsonPath("$.sourceTemplateId").value(templateId))
            .andExpect(jsonPath("$.items.length()").value(1))
            // The generated list starts uncompleted and without paid prices.
            .andExpect(jsonPath("$.items[0].completed").value(false))
            .andExpect(jsonPath("$.items[0].actualPrice").value(null))
            .andReturn().json()
        assertNotEquals(templateId, list.get("id").asString())

        mockMvc.perform(get("/api/v1/lists").with(user(owner)))
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    fun `creates a template from an existing list`() {
        val owner = newOwner()
        val listId = mockMvc.perform(
            post("/api/v1/lists").with(user(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Base","items":[{"name":"Eggs","categoryName":"Protein"}]}"""),
        ).andReturn().json().get("id").asString()

        mockMvc.perform(
            post("/api/v1/templates").with(user(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"From base","recurrence":"biweekly","sourceListId":"$listId"}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.templateRecurrence").value("biweekly"))
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].name").value("Eggs"))
    }

    @Test
    fun `template create retries by id are idempotent`() {
        val owner = newOwner()
        val templateId = UUID.randomUUID()
        val body = """{"id":"$templateId","name":"Sync","recurrence":"weekly","items":[{"name":"Milk","categoryName":"Dairy"}]}"""

        mockMvc.perform(post("/api/v1/templates").with(user(owner)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(templateId.toString()))
        mockMvc.perform(post("/api/v1/templates").with(user(owner)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.items.length()").value(1))
        mockMvc.perform(get("/api/v1/templates").with(user(owner)))
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    fun `templates cannot be finished as purchases`() {
        val owner = newOwner()
        val templateId = mockMvc.perform(
            post("/api/v1/templates").with(user(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Weekly","recurrence":"weekly"}"""),
        ).andReturn().json().get("id").asString()

        mockMvc.perform(post("/api/v1/lists/$templateId/finish").with(user(owner)))
            .andExpect(status().isForbidden)
    }
}
