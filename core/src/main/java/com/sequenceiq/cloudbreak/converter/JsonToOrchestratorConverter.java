package com.sequenceiq.cloudbreak.converter;


import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.OrchestratorRequest;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.json.Json;

@Component
public class JsonToOrchestratorConverter extends AbstractConversionServiceAwareConverter<OrchestratorRequest, Orchestrator> {

    @Override
    public Orchestrator convert(OrchestratorRequest source) {
        Orchestrator orchestrator = new Orchestrator();
        orchestrator.setApiEndpoint(source.getApiEndpoint());
        orchestrator.setType(source.getType());
        Map<String, Object> params = new HashMap<>();
        if (source.getParameters() != null && !source.getParameters().isEmpty()) {
            params = source.getParameters();
        }
        try {
            orchestrator.setAttributes(new Json(params));
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Invalid parameters", e);
        }

        return orchestrator;
    }
}
