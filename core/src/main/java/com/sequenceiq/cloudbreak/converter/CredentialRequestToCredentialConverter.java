package com.sequenceiq.cloudbreak.converter;

import static com.sequenceiq.cloudbreak.util.GovCloudFlagUtil.GOV_CLOUD_KEY;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.topology.TopologyService;
import com.sequenceiq.cloudbreak.util.GovCloudFlagUtil;

@Component
public class CredentialRequestToCredentialConverter extends AbstractConversionServiceAwareConverter<CredentialRequest, Credential> {

    @Inject
    private TopologyService topologyService;

    @Override
    public Credential convert(CredentialRequest source) {
        Credential credential = new Credential();
        credential.setName(source.getName());
        credential.setDescription(source.getDescription());
        credential.setCloudPlatform(source.getCloudPlatform());
        try {
            credential.setAttributes(new Json(source.getParameters()).getValue());
            if (source.getParameters() != null) {
                Object govCloudFlag = source.getParameters().get(GOV_CLOUD_KEY);
                credential.setGovCloud(GovCloudFlagUtil.extractGovCloudFlag(govCloudFlag));
            }
        } catch (JsonProcessingException ignored) {
            throw new BadRequestException("Invalid parameters");
        }
        if (source.getTopologyId() != null) {
            credential.setTopology(topologyService.get(source.getTopologyId()));
        }
        return credential;
    }

}
