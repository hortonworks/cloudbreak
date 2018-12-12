package com.sequenceiq.cloudbreak.converter;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static com.sequenceiq.cloudbreak.util.GovCloudFlagUtil.GOV_CLOUD_KEY;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.credential.CredentialValidator;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.stack.resource.definition.credential.CredentialDefinitionService;
import com.sequenceiq.cloudbreak.service.topology.TopologyService;
import com.sequenceiq.cloudbreak.util.GovCloudFlagUtil;

@Component
public class CredentialRequestToCredentialConverter extends AbstractConversionServiceAwareConverter<CredentialRequest, Credential> {

    @Inject
    private CredentialDefinitionService credentialDefinitionService;

    @Inject
    private TopologyService topologyService;

    @Inject
    private CredentialValidator credentialValidator;

    @Override
    public Credential convert(CredentialRequest source) {
        Credential credential = new Credential();
        credential.setName(source.getName());
        credential.setDescription(source.getDescription());
        credentialValidator.validateCredentialCloudPlatform(source.getCloudPlatform());
        String cloudPlatform = source.getCloudPlatform();
        credential.setCloudPlatform(cloudPlatform);
        Map<String, Object> parameters = credentialDefinitionService.processProperties(platform(cloudPlatform), source.getParameters());
        if (parameters != null && !parameters.isEmpty()) {
            try {
                credential.setAttributes(new Json(parameters));
                Object govCloudFlag = parameters.get(GOV_CLOUD_KEY);
                credential.setGovCloud(GovCloudFlagUtil.extractGovCloudFlag(govCloudFlag));
            } catch (JsonProcessingException ignored) {
                throw new BadRequestException("Invalid parameters");
            }
        }
        if (source.getTopologyId() != null) {
            credential.setTopology(topologyService.get(source.getTopologyId()));
        }
        return credential;
    }

}
