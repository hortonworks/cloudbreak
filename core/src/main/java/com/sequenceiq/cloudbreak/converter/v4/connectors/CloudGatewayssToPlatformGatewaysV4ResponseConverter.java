package com.sequenceiq.cloudbreak.converter.v4.connectors;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.CloudGatewayV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformGatewaysV4Response;
import com.sequenceiq.cloudbreak.cloud.model.CloudGateWay;
import com.sequenceiq.cloudbreak.cloud.model.CloudGateWays;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class CloudGatewayssToPlatformGatewaysV4ResponseConverter extends AbstractConversionServiceAwareConverter<CloudGateWays, PlatformGatewaysV4Response> {

    @Override
    public PlatformGatewaysV4Response convert(CloudGateWays source) {
        Map<String, Set<CloudGatewayV4Request>> result = new HashMap<>();
        for (Entry<String, Set<CloudGateWay>> entry : source.getCloudGateWayResponses().entrySet()) {
            Set<CloudGatewayV4Request> cloudGatewayJsons = new HashSet<>();
            for (CloudGateWay gateway : entry.getValue()) {
                CloudGatewayV4Request actual = new CloudGatewayV4Request(gateway.getName(), gateway.getId(), gateway.getProperties());
                cloudGatewayJsons.add(actual);
            }
            result.put(entry.getKey(), cloudGatewayJsons);
        }
        return new PlatformGatewaysV4Response(result);
    }
}
