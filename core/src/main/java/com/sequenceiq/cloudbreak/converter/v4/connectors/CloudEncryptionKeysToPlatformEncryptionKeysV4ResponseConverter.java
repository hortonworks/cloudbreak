package com.sequenceiq.cloudbreak.converter.v4.connectors;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.EncryptionKeyConfigV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformEncryptionKeysV4Response;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKey;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKeys;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class CloudEncryptionKeysToPlatformEncryptionKeysV4ResponseConverter
        extends AbstractConversionServiceAwareConverter<CloudEncryptionKeys, PlatformEncryptionKeysV4Response> {

    @Override
    public PlatformEncryptionKeysV4Response convert(CloudEncryptionKeys source) {
        PlatformEncryptionKeysV4Response platformEncryptionKeysV4Response = new PlatformEncryptionKeysV4Response();
        Set<EncryptionKeyConfigV4Response> result = new HashSet<>();
        for (CloudEncryptionKey entry : source.getCloudEncryptionKeys()) {
            EncryptionKeyConfigV4Response actual =
                    new EncryptionKeyConfigV4Response(entry.getName(), entry.getId(), entry.getDescription(), entry.getDisplayName(), entry.getProperties());
            result.add(actual);
        }
        platformEncryptionKeysV4Response.setEncryptionKeyConfigs(result);
        return platformEncryptionKeysV4Response;
    }
}
