package com.sequenceiq.environment.platformresource.v1.converter;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKey;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKeys;
import com.sequenceiq.environment.api.v1.platformresource.model.EncryptionKeyConfigResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformEncryptionKeysResponse;

@Component
public class CloudEncryptionKeysToPlatformEncryptionKeysV1ResponseConverter {

    public PlatformEncryptionKeysResponse convert(CloudEncryptionKeys source) {
        PlatformEncryptionKeysResponse platformEncryptionKeysResponse = new PlatformEncryptionKeysResponse();
        Set<EncryptionKeyConfigResponse> result = new HashSet<>();
        for (CloudEncryptionKey entry : source.getCloudEncryptionKeys()) {
            EncryptionKeyConfigResponse actual =
                    new EncryptionKeyConfigResponse(entry.getName(), entry.getId(), entry.getDescription(), entry.getDisplayName(), entry.getProperties());
            result.add(actual);
        }
        platformEncryptionKeysResponse.setEncryptionKeyConfigs(result);
        return platformEncryptionKeysResponse;
    }
}
