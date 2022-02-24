package com.sequenceiq.cloudbreak.converter.spi;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.dto.credential.Credential;

@Component
public class CredentialToCloudCredentialConverter {

    public CloudCredential convert(Credential credential) {
        Map<String, Object> fields = credential.getAttributes().getMap();
        return new CloudCredential(credential.getCrn(), credential.getName(), fields, credential.getAccount(), false);
    }
}
