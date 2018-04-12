package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.api.model.IpPoolJson;
import com.sequenceiq.cloudbreak.api.model.PlatformIpPoolsResponse;
import com.sequenceiq.cloudbreak.cloud.model.CloudIpPool;
import com.sequenceiq.cloudbreak.cloud.model.CloudIpPools;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

@Component
public class CloudIpPoolsToPlatformIpPoolsResponseConverter extends AbstractConversionServiceAwareConverter<CloudIpPools, PlatformIpPoolsResponse> {

    @Override
    public PlatformIpPoolsResponse convert(CloudIpPools source) {
        Map<String, Set<IpPoolJson>> result = new HashMap<>();
        for (Entry<String, Set<CloudIpPool>> entry : source.getCloudIpPools().entrySet()) {
            Set<IpPoolJson> ipPoolJsonSet = new HashSet<>();
            for (CloudIpPool ipPool : entry.getValue()) {
                IpPoolJson actual = new IpPoolJson(ipPool.getName(), ipPool.getId(), ipPool.getProperties());
                ipPoolJsonSet.add(actual);
            }
            result.put(entry.getKey(), ipPoolJsonSet);
        }
        return new PlatformIpPoolsResponse(result);
    }
}
