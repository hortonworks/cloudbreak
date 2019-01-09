package com.sequenceiq.cloudbreak.converter.v4.credentials;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.requests.CredentialV4Request;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.credential.CredentialPropertyCollector;
import com.sequenceiq.cloudbreak.service.topology.TopologyService;

@Component
public class CredentialRequestToCredentialConverter extends AbstractConversionServiceAwareConverter<CredentialV4Request, Credential> {

    @Inject
    private TopologyService topologyService;

    @Inject
    private CredentialPropertyCollector credentialPropertyCollector;

    @Override
    public Credential convert(CredentialV4Request source) {
        Credential credential = new Credential();
        credential.setName(source.getName());
        credential.setDescription(source.getDescription());
        credential.setCloudPlatform(source.getCloudPlatform());
        try {
            credential.setAttributes(new Json(credentialPropertyCollector.propertyMap(source)).getValue());
            if (source.getAws() != null) {
                credential.setGovCloud(source.getAws().getGovCloud());
            }
        } catch (JsonProcessingException ignored) {
            throw new BadRequestException("Invalid parameters");
        }
        return credential;
    }

}
