package com.sequenceiq.cloudbreak.service.environment;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKey;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKeys;
import com.sequenceiq.environment.api.v1.platformresource.PlatformResourceEndpoint;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformEncryptionKeysResponse;

@Service
public class PlatformResourceClientService {

    @Inject
    private PlatformResourceEndpoint platformResourceEndpoint;

    public CloudEncryptionKeys getEncryptionKeys(String credentialName, String region) {
        PlatformEncryptionKeysResponse encryptionKeys = platformResourceEndpoint.getEncryptionKeys(credentialName, region, null, null);
        Set<CloudEncryptionKey> keys = encryptionKeys.getEncryptionKeyConfigs().stream()
                .map(response -> new CloudEncryptionKey(response.getName(), response.getId(),
                        response.getDescription(), response.getDisplayName(), response.getProperties()))
                .collect(Collectors.toSet());
        return new CloudEncryptionKeys(keys);
    }
}
