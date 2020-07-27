package com.sequenceiq.cloudbreak.service.environment;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKey;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKeys;
import com.sequenceiq.environment.api.v1.platformresource.PlatformResourceEndpoint;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformEncryptionKeysResponse;

@Service
public class PlatformResourceClientService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformResourceClientService.class);

    @Inject
    private PlatformResourceEndpoint platformResourceEndpoint;

    public CloudEncryptionKeys getEncryptionKeys(String environmentCrn, String region) {
        LOGGER.info("Fetch encryption keys by environment crn: {} and region: {}", environmentCrn, region);
        PlatformEncryptionKeysResponse encryptionKeys = platformResourceEndpoint.getEncryptionKeys(null, null, region, null, null, environmentCrn);
        Set<CloudEncryptionKey> keys = encryptionKeys.getEncryptionKeyConfigs().stream()
                .map(response -> new CloudEncryptionKey(response.getName(), response.getId(),
                        response.getDescription(), response.getDisplayName(), response.getProperties()))
                .collect(Collectors.toSet());
        return new CloudEncryptionKeys(keys);
    }
}
