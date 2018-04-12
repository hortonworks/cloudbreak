package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.PlatformSecurityGroupResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformSecurityGroupsResponse;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroup;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroups;

@Component
public class CloudSecurityGroupsToPlatformSecurityGroupsResponseConverter
        extends AbstractConversionServiceAwareConverter<CloudSecurityGroups, PlatformSecurityGroupsResponse> {

    @Override
    public PlatformSecurityGroupsResponse convert(CloudSecurityGroups source) {
        Map<String, Set<PlatformSecurityGroupResponse>> result = new HashMap<>();
        for (Entry<String, Set<CloudSecurityGroup>> entry : source.getCloudSecurityGroupsResponses().entrySet()) {
            Set<PlatformSecurityGroupResponse> securityGroupResponses = new HashSet<>();
            for (CloudSecurityGroup securityGroup : entry.getValue()) {
                PlatformSecurityGroupResponse actual = new PlatformSecurityGroupResponse(
                        securityGroup.getGroupName(),
                        securityGroup.getGroupId(),
                        securityGroup.getProperties());
                securityGroupResponses.add(actual);
            }
            result.put(entry.getKey(), securityGroupResponses);
        }
        return new PlatformSecurityGroupsResponse(result);
    }
}
