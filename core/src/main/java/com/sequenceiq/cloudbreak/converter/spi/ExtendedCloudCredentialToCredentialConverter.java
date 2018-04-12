package com.sequenceiq.cloudbreak.converter.spi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.json.Json;
import org.springframework.stereotype.Component;

@Component
public class ExtendedCloudCredentialToCredentialConverter {

    public Credential convert(ExtendedCloudCredential extendedCloudCredential) {
        Credential credential = new Credential();
        credential.setId(extendedCloudCredential.getId());
        credential.setName(extendedCloudCredential.getName());
        credential.setDescription(extendedCloudCredential.getDescription());
        credential.setAccount(extendedCloudCredential.getAccount());
        credential.setOwner(extendedCloudCredential.getOwner());
        credential.setCloudPlatform(extendedCloudCredential.getCloudPlatform());
        try {
            Json json = new Json(extendedCloudCredential.getParameters());
            credential.setAttributes(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        return credential;
    }
}
