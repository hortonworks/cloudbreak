package com.sequenceiq.cloudbreak.converter.v4.connectors;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.IpPoolV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformIpPoolsV4Response;
import com.sequenceiq.cloudbreak.cloud.model.CloudIpPool;
import com.sequenceiq.cloudbreak.cloud.model.CloudIpPools;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class CloudIpPoolsToPlatformIpPoolsV4ResponseConverter extends AbstractConversionServiceAwareConverter<CloudIpPools, PlatformIpPoolsV4Response> {

    @Override
    public PlatformIpPoolsV4Response convert(CloudIpPools source) {
        Map<String, Set<IpPoolV4Response>> result = new HashMap<>();
        for (Entry<String, Set<CloudIpPool>> entry : source.getCloudIpPools().entrySet()) {
            Set<IpPoolV4Response> ipPoolJsonSet = new HashSet<>();
            for (CloudIpPool ipPool : entry.getValue()) {
                IpPoolV4Response actual = new IpPoolV4Response(ipPool.getName(), ipPool.getId(), ipPool.getProperties());
                ipPoolJsonSet.add(actual);
            }
            result.put(entry.getKey(), ipPoolJsonSet);
        }
        return new PlatformIpPoolsV4Response(result);
    }
}
