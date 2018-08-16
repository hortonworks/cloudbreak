package com.sequenceiq.cloudbreak.service.credential;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.json.Json;

@Service
public class CredentialMigrationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialMigrationService.class);

    @Inject
    private CredentialService credentialService;

    public void migrateGcpCredentials() {
        Set<Credential> gcpCredentials = credentialService.findAllByCloudPlatform(CloudConstants.GCP);
        Set<Credential> updatedCredentials = new HashSet<>();
        for (Credential credential : gcpCredentials) {
            Json attributes = credential.getAttributes();
            Map<String, Object> newAttributes = attributes.getMap();
            if (!newAttributes.containsKey("selector")) {
                newAttributes.put("selector", "credential-p12");
                try {
                    credential.setAttributes(new Json(newAttributes));
                    updatedCredentials.add(credential);
                } catch (IOException ex) {
                    LOGGER.error("Credential attributes update failed", ex);
                }
            }
        }
        if (!updatedCredentials.isEmpty()) {
            credentialService.saveAllCredential(updatedCredentials);
        }
    }
}
