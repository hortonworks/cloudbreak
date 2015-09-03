package com.sequenceiq.cloudbreak.converter.spi;

import static com.sequenceiq.cloudbreak.service.stack.flow.ReflectionUtils.getDeclaredFields;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.domain.Credential;

@Component
public class CredentialToCloudCredentialConverter {

    private static final String CREDENTIAL_ID = "id";

    public CloudCredential convert(Credential credential) {
        Map<String, Object> fields = getDeclaredFields(credential);
        fields.put(CREDENTIAL_ID, credential.getId());
        return new CloudCredential(credential.getName(), credential.getPublicKey(), fields);
    }

}
