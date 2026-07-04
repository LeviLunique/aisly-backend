package com.aisly.backend.architecture

import com.aisly.backend.AislyBackendApplication
import org.junit.jupiter.api.Test
import org.springframework.modulith.core.ApplicationModules

class ModulithArchitectureTest {
    @Test
    fun `application modules are valid`() {
        ApplicationModules.of(AislyBackendApplication::class.java).verify()
    }
}

