package com.sequenceiq.cloudbreak.converter.spi;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.service.credential.CredentialClientService;

@Component
public class CredentialToCloudCredentialConverter {

    @Inject
    private CredentialClientService credentialClientService;

    public CloudCredential convert(String credentialCrn) {
        return convert(credentialClientService.get(credentialCrn));
    }

    public CloudCredential convert(Credential credential) {
        Map<String, Object> fields = credential.getAttributes().getMap();
        return new CloudCredential(credential.getCrn(), credential.getName(), fields);
    }
}
