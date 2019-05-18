package com.sequenceiq.environment.platformresource.converter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudSshKey;
import com.sequenceiq.cloudbreak.cloud.model.CloudSshKeys;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.environment.api.platformresource.model.PlatformSshKeyV1Response;
import com.sequenceiq.environment.api.platformresource.model.PlatformSshKeysV1Response;

@Component
public class CloudSshKeysToPlatformSshKeysV1ResponseConverter extends AbstractConversionServiceAwareConverter<CloudSshKeys, PlatformSshKeysV1Response> {

    @Override
    public PlatformSshKeysV1Response convert(CloudSshKeys source) {
        Map<String, Set<PlatformSshKeyV1Response>> result = new HashMap<>();
        for (Entry<String, Set<CloudSshKey>> entry : source.getCloudSshKeysResponses().entrySet()) {
            Set<PlatformSshKeyV1Response> sshKeyResponses = new HashSet<>();
            for (CloudSshKey cloudSshKey : entry.getValue()) {
                PlatformSshKeyV1Response actual = new PlatformSshKeyV1Response(cloudSshKey.getName(), cloudSshKey.getProperties());
                sshKeyResponses.add(actual);
            }
            result.put(entry.getKey(), sshKeyResponses);
        }
        return new PlatformSshKeysV1Response(result);
    }
}
