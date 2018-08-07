package com.sequenceiq.cloudbreak.converter;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.EncryptionKeyConfigJson;
import com.sequenceiq.cloudbreak.api.model.PlatformEncryptionKeysResponse;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKey;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKeys;

@Component
public class CloudEncryptionKeysToPlatformEncryptionKeysResponseConverter
        extends AbstractConversionServiceAwareConverter<CloudEncryptionKeys, PlatformEncryptionKeysResponse> {

    @Override
    public PlatformEncryptionKeysResponse convert(CloudEncryptionKeys source) {
        PlatformEncryptionKeysResponse platformEncryptionKeysResponse = new PlatformEncryptionKeysResponse();
        Set<EncryptionKeyConfigJson> result = new HashSet<>();
        for (CloudEncryptionKey entry : source.getCloudEncryptionKeys()) {
            EncryptionKeyConfigJson actual =
                    new EncryptionKeyConfigJson(entry.getName(), entry.getId(), entry.getDescription(), entry.getDisplayName(), entry.getProperties());
            result.add(actual);
        }
        platformEncryptionKeysResponse.setEncryptionKeyConfigs(result);
        return platformEncryptionKeysResponse;
    }
}
