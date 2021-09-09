package com.sequenceiq.environment.platformresource.v1.converter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudSshKey;
import com.sequenceiq.cloudbreak.cloud.model.CloudSshKeys;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformSshKeyResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformSshKeysResponse;

@Component
public class CloudSshKeysToPlatformSshKeysV1ResponseConverter {

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
