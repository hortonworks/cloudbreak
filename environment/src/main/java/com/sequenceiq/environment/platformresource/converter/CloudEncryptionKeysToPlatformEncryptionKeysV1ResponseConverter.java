package com.sequenceiq.environment.platformresource.converter;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKey;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKeys;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.environment.api.platformresource.model.EncryptionKeyConfigV1Response;
import com.sequenceiq.environment.api.platformresource.model.PlatformEncryptionKeysV1Response;

@Component
public class CloudEncryptionKeysToPlatformEncryptionKeysV1ResponseConverter
        extends AbstractConversionServiceAwareConverter<CloudEncryptionKeys, PlatformEncryptionKeysV1Response> {

    @Override
    public PlatformEncryptionKeysV1Response convert(CloudEncryptionKeys source) {
        PlatformEncryptionKeysV1Response platformEncryptionKeysV1Response = new PlatformEncryptionKeysV1Response();
        Set<EncryptionKeyConfigV1Response> result = new HashSet<>();
        for (CloudEncryptionKey entry : source.getCloudEncryptionKeys()) {
            EncryptionKeyConfigV1Response actual =
                    new EncryptionKeyConfigV1Response(entry.getName(), entry.getId(), entry.getDescription(), entry.getDisplayName(), entry.getProperties());
            result.add(actual);
        }
        platformEncryptionKeysV1Response.setEncryptionKeyConfigs(result);
        return platformEncryptionKeysV1Response;
    }
}
