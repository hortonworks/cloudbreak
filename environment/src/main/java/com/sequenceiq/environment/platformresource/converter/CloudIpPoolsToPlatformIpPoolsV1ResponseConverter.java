package com.sequenceiq.environment.platformresource.converter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudIpPool;
import com.sequenceiq.cloudbreak.cloud.model.CloudIpPools;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.environment.api.platformresource.model.IpPoolV1Response;
import com.sequenceiq.environment.api.platformresource.model.PlatformIpPoolsV1Response;

@Component
public class CloudIpPoolsToPlatformIpPoolsV1ResponseConverter extends AbstractConversionServiceAwareConverter<CloudIpPools, PlatformIpPoolsV1Response> {

    @Override
    public PlatformIpPoolsV1Response convert(CloudIpPools source) {
        Map<String, Set<IpPoolV1Response>> result = new HashMap<>();
        for (Entry<String, Set<CloudIpPool>> entry : source.getCloudIpPools().entrySet()) {
            Set<IpPoolV1Response> ipPoolJsonSet = new HashSet<>();
            for (CloudIpPool ipPool : entry.getValue()) {
                IpPoolV1Response actual = new IpPoolV1Response(ipPool.getName(), ipPool.getId(), ipPool.getProperties());
                ipPoolJsonSet.add(actual);
            }
            result.put(entry.getKey(), ipPoolJsonSet);
        }
        return new PlatformIpPoolsV1Response(result);
    }
}
