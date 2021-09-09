package com.sequenceiq.environment.platformresource.v1.converter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudGateWay;
import com.sequenceiq.cloudbreak.cloud.model.CloudGateWays;
import com.sequenceiq.environment.api.v1.platformresource.model.CloudGatewayRequest;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformGatewaysResponse;

@Component
public class CloudGatewayssToPlatformGatewaysV1ResponseConverter {

    public PlatformGatewaysResponse convert(CloudGateWays source) {
        Map<String, Set<CloudGatewayRequest>> result = new HashMap<>();
        for (Entry<String, Set<CloudGateWay>> entry : source.getCloudGateWayResponses().entrySet()) {
            Set<CloudGatewayRequest> cloudGatewayJsons = new HashSet<>();
            for (CloudGateWay gateway : entry.getValue()) {
                CloudGatewayRequest actual = new CloudGatewayRequest(gateway.getName(), gateway.getId(), gateway.getProperties());
                cloudGatewayJsons.add(actual);
            }
            result.put(entry.getKey(), cloudGatewayJsons);
        }
        return new PlatformGatewaysResponse(result);
    }
}
