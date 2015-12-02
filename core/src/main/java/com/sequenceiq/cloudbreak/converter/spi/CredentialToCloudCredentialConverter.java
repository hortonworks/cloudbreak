package com.sequenceiq.cloudbreak.converter.spi;

import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;

import org.jasypt.encryption.pbe.PBEStringCleanablePasswordEncryptor;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.json.Json;

@Component
public class CredentialToCloudCredentialConverter {

    private static final String CREDENTIAL_ID = "id";
    private static final String USER_NAME = "userName";
    private static final String PASSWORD = "password";

    @Inject
    private PBEStringCleanablePasswordEncryptor encryptor;

    public CloudCredential convert(Credential credential) {
        Json attributes = credential.getAttributes();
        Map<String, Object> fields = attributes == null ? Collections.<String, Object>emptyMap() : attributes.getMap();
        for (String key : fields.keySet()) {
            //TODO decrypt other then based on specific keys
            if (USER_NAME.equals(key) || PASSWORD.equals(key)) {
                fields.put(key, encryptor.decrypt(String.valueOf(fields.get(key))));
            }
        }
        fields.put(CREDENTIAL_ID, credential.getId());
        return new CloudCredential(credential.getId(), credential.getName(), credential.getPublicKey(), credential.getLoginUserName(), fields);
    }

}
