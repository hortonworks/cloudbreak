package com.sequenceiq.cloudbreak.converter.v4.connectors;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformSshKeyV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformSshKeysV4Response;
import com.sequenceiq.cloudbreak.cloud.model.CloudSshKey;
import com.sequenceiq.cloudbreak.cloud.model.CloudSshKeys;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class CloudSshKeysToPlatformSshKeysV4ResponseConverter extends AbstractConversionServiceAwareConverter<CloudSshKeys, PlatformSshKeysV4Response> {

    @Override
    public PlatformSshKeysV4Response convert(CloudSshKeys source) {
        Map<String, Set<PlatformSshKeyV4Response>> result = new HashMap<>();
        for (Entry<String, Set<CloudSshKey>> entry : source.getCloudSshKeysResponses().entrySet()) {
            Set<PlatformSshKeyV4Response> sshKeyResponses = new HashSet<>();
            for (CloudSshKey cloudSshKey : entry.getValue()) {
                PlatformSshKeyV4Response actual = new PlatformSshKeyV4Response(cloudSshKey.getName(), cloudSshKey.getProperties());
                sshKeyResponses.add(actual);
            }
            result.put(entry.getKey(), sshKeyResponses);
        }
        return new PlatformSshKeysV4Response(result);
    }
}
