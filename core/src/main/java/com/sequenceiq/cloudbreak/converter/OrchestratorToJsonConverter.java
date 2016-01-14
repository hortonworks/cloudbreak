package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.OrchestratorResponse;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.json.Json;

@Component
public class OrchestratorToJsonConverter extends AbstractConversionServiceAwareConverter<Orchestrator, OrchestratorResponse> {

    @Override
    public OrchestratorResponse convert(Orchestrator source) {
        OrchestratorResponse orchestratorResponse = new OrchestratorResponse();
        orchestratorResponse.setType(source.getType());
        orchestratorResponse.setApiEndpoint(source.getApiEndpoint());
        Json attributes = source.getAttributes();
        if (attributes != null) {
            orchestratorResponse.setParameters(attributes.getMap());
        }
        return orchestratorResponse;
    }
}
