package com.aisly.backend

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class CatalogApiTests : ApiTestSupport() {

    @Test
    fun `archiving a catalog entry persists and hides it from the default listing`() {
        val owner = newOwner()

        val created = mockMvc.perform(
            post("/api/v1/catalog/items").with(user(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Banana","categoryName":"Produce","unit":"kg"}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.archived").value(false))
            .andReturn().json()
        val id = created.get("id").asString()

        // Archive it — the client sends archived=true on a full update (Items screen).
        mockMvc.perform(
            put("/api/v1/catalog/items/$id").with(user(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Banana","categoryName":"Produce","unit":"kg","archived":true}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.archived").value(true))

        // Default listing hides archived entries…
        mockMvc.perform(get("/api/v1/catalog/items").with(user(owner)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))

        // …but includeArchived=true still returns it, now flagged archived.
        mockMvc.perform(get("/api/v1/catalog/items?includeArchived=true").with(user(owner)))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].archived").value(true))
    }

    @Test
    fun `unarchiving a catalog entry brings it back to the default listing`() {
        val owner = newOwner()
        val created = mockMvc.perform(
            post("/api/v1/catalog/items").with(user(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Rice","categoryName":"Pantry","archived":true}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.archived").value(true))
            .andReturn().json()
        val id = created.get("id").asString()

        // Created as archived → absent from the default listing.
        mockMvc.perform(get("/api/v1/catalog/items").with(user(owner)))
            .andExpect(jsonPath("$.length()").value(0))

        mockMvc.perform(
            put("/api/v1/catalog/items/$id").with(user(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Rice","categoryName":"Pantry","archived":false}"""),
        )
            .andExpect(jsonPath("$.archived").value(false))

        mockMvc.perform(get("/api/v1/catalog/items").with(user(owner)))
            .andExpect(jsonPath("$.length()").value(1))
    }
}
