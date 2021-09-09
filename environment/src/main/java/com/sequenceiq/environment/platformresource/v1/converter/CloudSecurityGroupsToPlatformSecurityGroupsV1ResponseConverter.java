package com.sequenceiq.environment.platformresource.v1.converter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroup;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroups;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformSecurityGroupResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformSecurityGroupsResponse;

@Component
public class CloudSecurityGroupsToPlatformSecurityGroupsV1ResponseConverter {

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
