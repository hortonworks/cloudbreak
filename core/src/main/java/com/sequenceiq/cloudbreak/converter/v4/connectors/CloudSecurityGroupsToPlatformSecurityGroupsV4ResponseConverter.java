package com.sequenceiq.cloudbreak.converter.v4.connectors;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformSecurityGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformSecurityGroupsV4Response;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroup;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroups;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class CloudSecurityGroupsToPlatformSecurityGroupsV4ResponseConverter
        extends AbstractConversionServiceAwareConverter<CloudSecurityGroups, PlatformSecurityGroupsV4Response> {

    @Override
    public PlatformSecurityGroupsV4Response convert(CloudSecurityGroups source) {
        Map<String, Set<PlatformSecurityGroupV4Response>> result = new HashMap<>();
        for (Entry<String, Set<CloudSecurityGroup>> entry : source.getCloudSecurityGroupsResponses().entrySet()) {
            Set<PlatformSecurityGroupV4Response> securityGroupResponses = new HashSet<>();
            for (CloudSecurityGroup securityGroup : entry.getValue()) {
                PlatformSecurityGroupV4Response actual = new PlatformSecurityGroupV4Response(
                        securityGroup.getGroupName(),
                        securityGroup.getGroupId(),
                        securityGroup.getProperties());
                securityGroupResponses.add(actual);
            }
            result.put(entry.getKey(), securityGroupResponses);
        }
        return new PlatformSecurityGroupsV4Response(result);
    }
}
