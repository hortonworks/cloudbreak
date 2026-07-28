package com.sequenceiq.cloudbreak.converter.spi;

import static com.sequenceiq.cloudbreak.cloud.model.CloudCredential.GOV_CLOUD;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.dto.credential.Credential;

@Component
public class CredentialToCloudCredentialConverter {

    public CloudCredential convert(Credential credential) {
        Map<String, Object> fields = credential.getAttributes().getMap();
        fields.put(GOV_CLOUD, credential.isGovCloud());
        return new CloudCredential(credential.getCrn(), credential.getName(), fields, credential.getAccount());
    }
}
