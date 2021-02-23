package com.sequenceiq.it.cloudbreak.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;

public class InstanceGroupUtil {

    private InstanceGroupUtil() {

    }

    public static List<String> getInstanceIds(List<InstanceGroupV4Response> instanceGroupV4Responses, String hostGroupName) {
        List<String> instanceIds = new ArrayList<>();
        InstanceGroupV4Response instanceGroupV4Response = instanceGroupV4Responses.stream().filter(instanceGroup ->
                instanceGroup.getName().equals(hostGroupName)).findFirst().orElse(null);
        InstanceMetaDataV4Response instanceMetaDataV4Response = Objects.requireNonNull(instanceGroupV4Response)
                .getMetadata().stream().findFirst().orElse(null);
        instanceIds.add(Objects.requireNonNull(instanceMetaDataV4Response).getInstanceId());
        return instanceIds;
    }
}
