package com.sequenceiq.environment.platformresource.converter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudGateWay;
import com.sequenceiq.cloudbreak.cloud.model.CloudGateWays;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.environment.api.platformresource.model.CloudGatewayV1Request;
import com.sequenceiq.environment.api.platformresource.model.PlatformGatewaysV1Response;

@Component
public class CloudGatewayssToPlatformGatewaysV1ResponseConverter extends AbstractConversionServiceAwareConverter<CloudGateWays, PlatformGatewaysV1Response> {

    @Override
    public PlatformGatewaysV1Response convert(CloudGateWays source) {
        Map<String, Set<CloudGatewayV1Request>> result = new HashMap<>();
        for (Entry<String, Set<CloudGateWay>> entry : source.getCloudGateWayResponses().entrySet()) {
            Set<CloudGatewayV1Request> cloudGatewayJsons = new HashSet<>();
            for (CloudGateWay gateway : entry.getValue()) {
                CloudGatewayV1Request actual = new CloudGatewayV1Request(gateway.getName(), gateway.getId(), gateway.getProperties());
                cloudGatewayJsons.add(actual);
            }
            result.put(entry.getKey(), cloudGatewayJsons);
        }
        return new PlatformGatewaysV1Response(result);
    }
}
