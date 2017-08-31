package com.sequenceiq.cloudbreak.converter;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.stack.resource.definition.credential.CredentialDefinitionService;
import com.sequenceiq.cloudbreak.service.topology.TopologyService;

@Component
public class JsonToCredentialConverter extends AbstractConversionServiceAwareConverter<CredentialRequest, Credential> {

    @Inject
    private CredentialDefinitionService credentialDefinitionService;

    @Inject
    private TopologyService topologyService;

    @Override
    public Credential convert(CredentialRequest source) {
        Credential credential = new Credential();
        credential.setName(source.getName());
        credential.setDescription(source.getDescription());
        String cloudPlatform = source.getCloudPlatform();
        credential.setCloudPlatform(cloudPlatform);
        Map<String, Object> parameters = credentialDefinitionService.processProperties(platform(cloudPlatform), source.getParameters());
        if (parameters != null && !parameters.isEmpty()) {
            try {
                credential.setAttributes(new Json(parameters));
            } catch (JsonProcessingException e) {
                throw new BadRequestException("Invalid parameters");
            }
        }
        if (source.getTopologyId() != null) {
            credential.setTopology(topologyService.getById(source.getTopologyId()));
        }
        return credential;
    }

}
