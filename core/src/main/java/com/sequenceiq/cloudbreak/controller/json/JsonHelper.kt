package com.sequenceiq.cloudbreak.controller.json

import java.io.IOException

import org.springframework.stereotype.Component

import com.fasterxml.jackson.databind.JsonNode
import com.sequenceiq.cloudbreak.controller.CloudbreakApiException
import com.sequenceiq.cloudbreak.util.JsonUtil

@Component
class JsonHelper {

    fun createJsonFromString(jsonString: String): JsonNode {
        try {
            return JsonUtil.readTree(jsonString)
        } catch (e: IOException) {
            throw CloudbreakApiException("Failed to parse JSON string.", e)
        }

    }

}
