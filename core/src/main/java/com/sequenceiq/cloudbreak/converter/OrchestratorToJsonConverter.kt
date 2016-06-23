package com.sequenceiq.cloudbreak.converter

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.OrchestratorResponse
import com.sequenceiq.cloudbreak.domain.Orchestrator
import com.sequenceiq.cloudbreak.domain.json.Json

@Component
class OrchestratorToJsonConverter : AbstractConversionServiceAwareConverter<Orchestrator, OrchestratorResponse>() {

    override fun convert(source: Orchestrator): OrchestratorResponse {
        val orchestratorResponse = OrchestratorResponse()
        orchestratorResponse.type = source.type
        orchestratorResponse.apiEndpoint = source.apiEndpoint
        val attributes = source.attributes
        if (attributes != null) {
            orchestratorResponse.parameters = attributes.map
        }
        return orchestratorResponse
    }
}
