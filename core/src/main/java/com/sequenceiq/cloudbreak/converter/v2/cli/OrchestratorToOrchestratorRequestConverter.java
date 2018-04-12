package com.sequenceiq.cloudbreak.converter.v2.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.OrchestratorRequest;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Orchestrator;

@Component
public class OrchestratorToOrchestratorRequestConverter extends AbstractConversionServiceAwareConverter<Orchestrator, OrchestratorRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrchestratorToOrchestratorRequestConverter.class);

    @Override
    public OrchestratorRequest convert(Orchestrator source) {
        OrchestratorRequest orchestratorRequest = new OrchestratorRequest();
        orchestratorRequest.setType(source.getType());
        orchestratorRequest.setParameters(null);
        return orchestratorRequest;
    }

}
