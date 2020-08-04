package com.sequenceiq.cloudbreak.service.environment;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKey;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKeys;
import com.sequenceiq.environment.api.v1.platformresource.EnvironmentPlatformResourceEndpoint;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformEncryptionKeysResponse;

@Service
public class PlatformResourceClientService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformResourceClientService.class);

    @Inject
    private EnvironmentPlatformResourceEndpoint environmentPlatformResourceEndpoint;

    public CloudEncryptionKeys getEncryptionKeys(String envCrn, String region) {
        LOGGER.info("Fetch encryption keys by environment crn: {} and region: {}", envCrn, region);
        PlatformEncryptionKeysResponse encryptionKeys = environmentPlatformResourceEndpoint.getEncryptionKeys(envCrn, region, null, null);
        Set<CloudEncryptionKey> keys = encryptionKeys.getEncryptionKeyConfigs().stream()
                .map(response -> new CloudEncryptionKey(response.getName(), response.getId(),
                        response.getDescription(), response.getDisplayName(), response.getProperties()))
                .collect(Collectors.toSet());
        return new CloudEncryptionKeys(keys);
    }
}
