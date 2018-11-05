package com.sequenceiq.cloudbreak.converter.spi;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.json.Json;

@Component
public class ExtendedCloudCredentialToCredentialConverter {

    public Credential convert(ExtendedCloudCredential extendedCloudCredential) {
        Credential credential = new Credential();
        credential.setId(extendedCloudCredential.getId());
        credential.setName(extendedCloudCredential.getName());
        credential.setDescription(extendedCloudCredential.getDescription());
        credential.setCloudPlatform(extendedCloudCredential.getCloudPlatform());
        try {
            Json json = new Json(extendedCloudCredential.getParameters());
            credential.setAttributes(json.getValue());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        return credential;
    }
}
