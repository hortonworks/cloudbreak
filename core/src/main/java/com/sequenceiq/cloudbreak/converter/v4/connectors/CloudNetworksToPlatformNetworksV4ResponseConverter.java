package com.sequenceiq.cloudbreak.converter.v4.connectors;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformNetworkV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformNetworksV4Response;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class CloudNetworksToPlatformNetworksV4ResponseConverter extends AbstractConversionServiceAwareConverter<CloudNetworks, PlatformNetworksV4Response> {

    @Override
    public PlatformNetworksV4Response convert(CloudNetworks source) {
        Map<String, Set<PlatformNetworkV4Response>> result = new HashMap<>();
        for (Entry<String, Set<CloudNetwork>> entry : source.getCloudNetworkResponses().entrySet()) {
            Set<PlatformNetworkV4Response> networks = new HashSet<>();
            for (CloudNetwork cloudNetwork : entry.getValue()) {
                PlatformNetworkV4Response actual = new PlatformNetworkV4Response(
                        cloudNetwork.getName(),
                        cloudNetwork.getId(),
                        cloudNetwork.getSubnets(),
                        cloudNetwork.getProperties());
                networks.add(actual);
            }
            result.put(entry.getKey(), networks);
        }
        return new PlatformNetworksV4Response(result);
    }
}
