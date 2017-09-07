package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.PlatformNetworkResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformNetworksResponse;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;

@Component
public class CloudNetworksToPlatformNetworksResponseConverter extends AbstractConversionServiceAwareConverter<CloudNetworks, PlatformNetworksResponse> {

    @Override
    public PlatformNetworksResponse convert(CloudNetworks source) {
        Map<String, Set<PlatformNetworkResponse>> result = new HashMap<>();
        for (Entry<String, Set<CloudNetwork>> entry : source.getCloudNetworkResponses().entrySet()) {
            Set<PlatformNetworkResponse> networks = new HashSet<>();
            for (CloudNetwork cloudNetwork : entry.getValue()) {
                PlatformNetworkResponse actual = new PlatformNetworkResponse(cloudNetwork.getName(), cloudNetwork.getSubnetIds(), cloudNetwork.getProperties());
                networks.add(actual);
            }
            result.put(entry.getKey(), networks);
        }
        return new PlatformNetworksResponse(result);
    }
}
