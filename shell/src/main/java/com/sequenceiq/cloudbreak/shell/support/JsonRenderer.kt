package com.sequenceiq.cloudbreak.shell.support

import org.springframework.stereotype.Component

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper

@Component
class JsonRenderer {

    private val objectMapper: ObjectMapper

    init {
        this.objectMapper = ObjectMapper()
    }

    @Throws(JsonProcessingException::class)
    fun render(`object`: Any): String {
        return objectMapper.writeValueAsString(`object`)
    }
}
