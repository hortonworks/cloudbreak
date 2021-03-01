package com.sequenceiq.it.cloudbreak.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;

public class InstanceUtil {
    private InstanceUtil() {
    }

    public static Map<String, InstanceStatus> getHealthyDistroXInstances() {
        return getInstanceStatuses(InstanceStatus.SERVICES_HEALTHY, HostGroupType.MASTER, HostGroupType.COMPUTE, HostGroupType.WORKER);
    }

    public static Map<String, InstanceStatus> getHealthySDXInstances() {
        return getInstanceStatuses(InstanceStatus.SERVICES_HEALTHY, HostGroupType.MASTER, HostGroupType.IDBROKER);
    }

    public static Map<String, InstanceStatus> getInstanceStatuses(InstanceStatus status, HostGroupType... hostGroupTypes) {
        return List.of(hostGroupTypes).stream()
                .collect(Collectors.toMap(hostGroupType -> hostGroupType.getName(), hostGroupType -> status));
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
