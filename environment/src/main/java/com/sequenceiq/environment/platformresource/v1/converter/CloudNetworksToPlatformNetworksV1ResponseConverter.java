package com.sequenceiq.environment.platformresource.v1.converter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformNetworkResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformNetworksResponse;

@Component
public class CloudNetworksToPlatformNetworksV1ResponseConverter {

    public PlatformNetworksResponse convert(CloudNetworks source) {
        Map<String, Set<PlatformNetworkResponse>> result = new HashMap<>();
        for (Entry<String, Set<CloudNetwork>> entry : source.getCloudNetworkResponses().entrySet()) {
            Set<PlatformNetworkResponse> networks = new HashSet<>();
            for (CloudNetwork cloudNetwork : entry.getValue()) {
                PlatformNetworkResponse actual = new PlatformNetworkResponse(
                        cloudNetwork.getName(),
                        cloudNetwork.getId(),
                        cloudNetwork.getSubnets(),
                        cloudNetwork.getSubnetsWithMetadata(),
                        cloudNetwork.getProperties());
                networks.add(actual);
            }
            result.put(entry.getKey(), networks);
        }
        return new PlatformNetworksResponse(result);
    }
}
