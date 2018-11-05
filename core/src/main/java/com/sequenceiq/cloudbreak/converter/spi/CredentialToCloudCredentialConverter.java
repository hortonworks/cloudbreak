package com.sequenceiq.cloudbreak.converter.spi;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.VaultService;

@Component
public class CredentialToCloudCredentialConverter {

    private static final String CREDENTIAL_ID = "id";

    @Inject
    private VaultService vaultService;

    public CloudCredential convert(Credential credential) {
        if (credential == null) {
            return null;
        }
        Json attributesFromVault = new Json(vaultService.resolveSingleValue(credential.getAttributes()));
        Map<String, Object> fields = attributesFromVault.getMap();
        fields.put(CREDENTIAL_ID, credential.getId());
        return new CloudCredential(credential.getId(), credential.getName(), fields);
    }

}
