package com.sequenceiq.cloudbreak.converter.spi;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.dto.credential.Credential;

@Component
public class ExtendedCloudCredentialToCredentialConverter {

    public Credential convert(ExtendedCloudCredential extendedCloudCredential) {
        Credential.Builder builder = Credential.builder()
                .name(extendedCloudCredential.getName())
                .description(extendedCloudCredential.getDescription())
                .cloudPlatform(extendedCloudCredential.getCloudPlatform())
                .crn(extendedCloudCredential.getId());
        try {
            Json json = new Json(extendedCloudCredential.getParameters());
            builder.attributes(json);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(e);
        }

        return builder.build();
    }
}
