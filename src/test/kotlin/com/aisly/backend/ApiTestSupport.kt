package com.aisly.backend

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.RequestPostProcessor
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class ApiTestSupport {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    /** Authenticates the request as the given user, exactly like a JWT issued by the AuthServer would. */
    fun user(subject: String): RequestPostProcessor = jwt().jwt { it.subject(subject) }

    /** A fresh owner per test keeps the shared in-memory database isolated between tests. */
    fun newOwner(): String = "owner-${UUID.randomUUID()}"

    fun MvcResult.json(): JsonNode = objectMapper.readTree(response.contentAsString)
}
