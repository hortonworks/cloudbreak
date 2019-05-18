package com.sequenceiq.environment.platformresource.converter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.environment.api.platformresource.model.PlatformNetworkV1Response;
import com.sequenceiq.environment.api.platformresource.model.PlatformNetworksV1Response;

@Component
public class CloudNetworksToPlatformNetworksV1ResponseConverter extends AbstractConversionServiceAwareConverter<CloudNetworks, PlatformNetworksV1Response> {

    @Override
    public PlatformNetworksV1Response convert(CloudNetworks source) {
        Map<String, Set<PlatformNetworkV1Response>> result = new HashMap<>();
        for (Entry<String, Set<CloudNetwork>> entry : source.getCloudNetworkResponses().entrySet()) {
            Set<PlatformNetworkV1Response> networks = new HashSet<>();
            for (CloudNetwork cloudNetwork : entry.getValue()) {
                PlatformNetworkV1Response actual = new PlatformNetworkV1Response(
                        cloudNetwork.getName(),
                        cloudNetwork.getId(),
                        cloudNetwork.getSubnets(),
                        cloudNetwork.getProperties());
                networks.add(actual);
            }
            result.put(entry.getKey(), networks);
        }
        return new PlatformNetworksV1Response(result);
    }
}
