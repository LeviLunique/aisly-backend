package com.aisly.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class AislyBackendApplication

fun main(args: Array<String>) {
    runApplication<AislyBackendApplication>(*args)
}

