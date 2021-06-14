package com.sequenceiq.it.cloudbreak.util;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;

public class InstanceUtil {
    private InstanceUtil() {
    }

    public static List<String> getInstanceIds(List<InstanceGroupV4Response> instanceGroupV4Responses, String hostGroupName) {
        InstanceGroupV4Response instanceGroupV4Response = instanceGroupV4Responses.stream().filter(instanceGroup ->
                instanceGroup.getName().equals(hostGroupName)).findFirst().orElse(null);
        return Objects.requireNonNull(instanceGroupV4Response)
                .getMetadata().stream().map(InstanceMetaDataV4Response::getInstanceId).collect(Collectors.toList());
    }
}
