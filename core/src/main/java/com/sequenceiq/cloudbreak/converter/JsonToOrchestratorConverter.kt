package com.sequenceiq.cloudbreak.converter


import java.util.HashMap

import org.springframework.stereotype.Component

import com.fasterxml.jackson.core.JsonProcessingException
import com.sequenceiq.cloudbreak.api.model.OrchestratorRequest
import com.sequenceiq.cloudbreak.controller.BadRequestException
import com.sequenceiq.cloudbreak.domain.Orchestrator
import com.sequenceiq.cloudbreak.domain.json.Json

@Component
class JsonToOrchestratorConverter : AbstractConversionServiceAwareConverter<OrchestratorRequest, Orchestrator>() {

    override fun convert(source: OrchestratorRequest): Orchestrator {
        val orchestrator = Orchestrator()
        orchestrator.apiEndpoint = source.apiEndpoint
        orchestrator.type = source.type
        var params: Map<String, Any> = HashMap()
        if (source.parameters != null && !source.parameters.isEmpty()) {
            params = source.parameters
        }
        try {
            orchestrator.attributes = Json(params)
        } catch (e: JsonProcessingException) {
            throw BadRequestException("Invalid parameters", e)
        }

        return orchestrator
    }
}
