package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.api.model.PlatformSshKeyResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformSshKeysResponse;
import com.sequenceiq.cloudbreak.cloud.model.CloudSshKey;
import com.sequenceiq.cloudbreak.cloud.model.CloudSshKeys;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

@Component
public class CloudSshKeysToPlatformSshKeysResponseConverter extends AbstractConversionServiceAwareConverter<CloudSshKeys, PlatformSshKeysResponse> {

    @Override
    public PlatformSshKeysResponse convert(CloudSshKeys source) {
        Map<String, Set<PlatformSshKeyResponse>> result = new HashMap<>();
        for (Entry<String, Set<CloudSshKey>> entry : source.getCloudSshKeysResponses().entrySet()) {
            Set<PlatformSshKeyResponse> sshKeyResponses = new HashSet<>();
            for (CloudSshKey cloudSshKey : entry.getValue()) {
                PlatformSshKeyResponse actual = new PlatformSshKeyResponse(cloudSshKey.getName(), cloudSshKey.getProperties());
                sshKeyResponses.add(actual);
            }
            result.put(entry.getKey(), sshKeyResponses);
        }
        return new PlatformSshKeysResponse(result);
    }
}
