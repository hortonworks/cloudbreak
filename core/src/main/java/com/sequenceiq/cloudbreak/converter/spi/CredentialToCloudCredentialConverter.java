package com.sequenceiq.cloudbreak.converter.spi;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.HashMap;
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
        Map<String, Object> fields;
        if (credential.getId() == null) {
            fields = isBlank(credential.getAttributes()) ? new HashMap<>() : new Json(credential.getAttributes()).getMap();
        } else {
            fields = new Json(vaultService.resolveSingleValue(credential.getAttributes())).getMap();
        }
        fields.put(CREDENTIAL_ID, credential.getId());
        return new CloudCredential(credential.getId(), credential.getName(), fields);
    }

}
