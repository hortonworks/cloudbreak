package com.sequenceiq.environment.platformresource.converter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroup;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroups;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.environment.api.platformresource.model.PlatformSecurityGroupV1Response;
import com.sequenceiq.environment.api.platformresource.model.PlatformSecurityGroupsV1Response;

@Component
public class CloudSecurityGroupsToPlatformSecurityGroupsV1ResponseConverter
        extends AbstractConversionServiceAwareConverter<CloudSecurityGroups, PlatformSecurityGroupsV1Response> {

    @Override
    public PlatformSecurityGroupsV1Response convert(CloudSecurityGroups source) {
        Map<String, Set<PlatformSecurityGroupV1Response>> result = new HashMap<>();
        for (Entry<String, Set<CloudSecurityGroup>> entry : source.getCloudSecurityGroupsResponses().entrySet()) {
            Set<PlatformSecurityGroupV1Response> securityGroupResponses = new HashSet<>();
            for (CloudSecurityGroup securityGroup : entry.getValue()) {
                PlatformSecurityGroupV1Response actual = new PlatformSecurityGroupV1Response(
                        securityGroup.getGroupName(),
                        securityGroup.getGroupId(),
                        securityGroup.getProperties());
                securityGroupResponses.add(actual);
            }
            result.put(entry.getKey(), securityGroupResponses);
        }
        return new PlatformSecurityGroupsV1Response(result);
    }
}
