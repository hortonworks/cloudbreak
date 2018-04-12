package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.api.model.CloudGatewayJson;
import com.sequenceiq.cloudbreak.api.model.PlatformGatewaysResponse;
import com.sequenceiq.cloudbreak.cloud.model.CloudGateWay;
import com.sequenceiq.cloudbreak.cloud.model.CloudGateWays;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

@Component
public class CloudGatewayssToPlatformGatewaysResponseConverter extends AbstractConversionServiceAwareConverter<CloudGateWays, PlatformGatewaysResponse> {

    @Override
    public PlatformGatewaysResponse convert(CloudGateWays source) {
        Map<String, Set<CloudGatewayJson>> result = new HashMap<>();
        for (Entry<String, Set<CloudGateWay>> entry : source.getCloudGateWayResponses().entrySet()) {
            Set<CloudGatewayJson> cloudGatewayJsons = new HashSet<>();
            for (CloudGateWay gateway : entry.getValue()) {
                CloudGatewayJson actual = new CloudGatewayJson(gateway.getName(), gateway.getId(), gateway.getProperties());
                cloudGatewayJsons.add(actual);
            }
            result.put(entry.getKey(), cloudGatewayJsons);
        }
        return new PlatformGatewaysResponse(result);
    }
}
