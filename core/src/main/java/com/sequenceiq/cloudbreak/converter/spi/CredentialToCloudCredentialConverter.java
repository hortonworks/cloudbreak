package com.sequenceiq.cloudbreak.converter.spi;

import static com.sequenceiq.cloudbreak.service.stack.flow.ReflectionUtils.getDeclaredFields;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.domain.Credential;

@Component
public class CredentialToCloudCredentialConverter {

    public CloudCredential convert(Credential credential) {
        Map<String, Object> fields = getDeclaredFields(credential);
        return new CloudCredential(credential.getId(), credential.getName(), credential.getPublicKey(), credential.getLoginUserName(), fields);
    }

}
