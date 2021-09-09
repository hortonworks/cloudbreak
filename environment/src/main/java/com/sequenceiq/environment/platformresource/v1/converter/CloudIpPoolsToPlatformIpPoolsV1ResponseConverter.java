package com.sequenceiq.environment.platformresource.v1.converter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudIpPool;
import com.sequenceiq.cloudbreak.cloud.model.CloudIpPools;
import com.sequenceiq.environment.api.v1.platformresource.model.IpPoolResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformIpPoolsResponse;

@Component
public class CloudIpPoolsToPlatformIpPoolsV1ResponseConverter {

    public PlatformIpPoolsResponse convert(CloudIpPools source) {
        Map<String, Set<IpPoolResponse>> result = new HashMap<>();
        for (Entry<String, Set<CloudIpPool>> entry : source.getCloudIpPools().entrySet()) {
            Set<IpPoolResponse> ipPoolJsonSet = new HashSet<>();
            for (CloudIpPool ipPool : entry.getValue()) {
                IpPoolResponse actual = new IpPoolResponse(ipPool.getName(), ipPool.getId(), ipPool.getProperties());
                ipPoolJsonSet.add(actual);
            }
            result.put(entry.getKey(), ipPoolJsonSet);
        }
        return new PlatformIpPoolsResponse(result);
    }
}
