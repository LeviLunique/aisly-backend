package com.aisly.backend

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

class CategoryApiTests : ApiTestSupport() {

    @Test
    fun `first access seeds the default categories with a fixed Other`() {
        val owner = newOwner()
        mockMvc.perform(get("/api/v1/categories").with(user(owner)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(7))
            .andExpect(jsonPath("$[0].name").value("Produce"))
            .andExpect(jsonPath("$[6].name").value("Other"))
            .andExpect(jsonPath("$[6].fixed").value(true))

        // Reading again does not seed twice.
        mockMvc.perform(get("/api/v1/categories").with(user(owner)))
            .andExpect(jsonPath("$.length()").value(7))
    }

    @Test
    fun `the fixed category can be neither edited nor deleted`() {
        val owner = newOwner()
        val categories = mockMvc.perform(get("/api/v1/categories").with(user(owner))).andReturn().json()
        val otherId = categories.first { it.get("fixed").asBoolean() }.get("id").asString()

        mockMvc.perform(delete("/api/v1/categories/$otherId").with(user(owner)))
            .andExpect(status().isForbidden)
        mockMvc.perform(
            put("/api/v1/categories/$otherId").with(user(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Renamed","iconName":"tag","colorHex":1}"""),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `custom categories support upsert by id and deletion`() {
        val owner = newOwner()
        mockMvc.perform(get("/api/v1/categories").with(user(owner))).andExpect(status().isOk)

        val categoryId = UUID.randomUUID()
        mockMvc.perform(
            post("/api/v1/categories").with(user(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"id":"$categoryId","name":"Feira","iconName":"leaf","colorHex":123}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(categoryId.toString()))
            .andExpect(jsonPath("$.fixed").value(false))

        // Retrying with the same id updates instead of duplicating.
        mockMvc.perform(
            post("/api/v1/categories").with(user(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"id":"$categoryId","name":"Feira Livre","iconName":"leaf","colorHex":123}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("Feira Livre"))
        mockMvc.perform(get("/api/v1/categories").with(user(owner)))
            .andExpect(jsonPath("$.length()").value(8))

        mockMvc.perform(delete("/api/v1/categories/$categoryId").with(user(owner)))
            .andExpect(status().isNoContent)
        mockMvc.perform(get("/api/v1/categories").with(user(owner)))
            .andExpect(jsonPath("$.length()").value(7))
    }

    @Test
    fun `a category id owned by another user is reported as 404`() {
        val ownerA = newOwner()
        val ownerB = newOwner()
        val categoryId = UUID.randomUUID()
        mockMvc.perform(
            post("/api/v1/categories").with(user(ownerA))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"id":"$categoryId","name":"Feira","iconName":"leaf","colorHex":123}"""),
        ).andExpect(status().isCreated)

        mockMvc.perform(
            post("/api/v1/categories").with(user(ownerB))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"id":"$categoryId","name":"Steal","iconName":"tag","colorHex":1}"""),
        ).andExpect(status().isNotFound)
    }

    @Test
    fun `reorder must include every non-fixed category`() {
        val owner = newOwner()
        val categories = mockMvc.perform(get("/api/v1/categories").with(user(owner))).andReturn().json()
        val movableIds = categories.filterNot { it.get("fixed").asBoolean() }.map { it.get("id").asString() }

        mockMvc.perform(
            put("/api/v1/categories/reorder").with(user(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"ids":[${movableIds.reversed().joinToString(",") { "\"$it\"" }}]}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value("Frozen"))

        mockMvc.perform(
            put("/api/v1/categories/reorder").with(user(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"ids":["${movableIds.first()}"]}"""),
        )
            .andExpect(status().isBadRequest)
    }
}
