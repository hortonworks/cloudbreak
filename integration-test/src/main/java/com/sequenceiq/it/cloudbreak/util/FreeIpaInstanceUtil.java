package com.sequenceiq.it.cloudbreak.util;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetaDataResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

public class FreeIpaInstanceUtil {
    private FreeIpaInstanceUtil() {
    }

    public static List<String> getInstanceIds(List<InstanceGroupResponse> instanceGroupResponses, String hostGroupName) {
        InstanceGroupResponse instanceGroupResponse = instanceGroupResponses.stream().filter(instanceGroup ->
                instanceGroup.getName().equals(hostGroupName)).findFirst().orElse(null);
        return Objects.requireNonNull(instanceGroupResponse).getMetaData()
                .stream().map(InstanceMetaDataResponse::getInstanceId).collect(Collectors.toList());
    }

    public static Map<List<String>, InstanceStatus> getInstanceStatusMap(DescribeFreeIpaResponse freeIpaResponse) {
        return freeIpaResponse.getInstanceGroups().stream()
                .filter(instanceGroupResponse -> instanceGroupResponse.getMetaData().stream()
                        .anyMatch(instanceMetaDataResponse -> Objects.nonNull(instanceMetaDataResponse.getInstanceId())))
                .collect(Collectors.toMap(
                        instanceGroupResponse -> instanceGroupResponse.getMetaData().stream()
                                .map(InstanceMetaDataResponse::getInstanceId)
                                .filter(Objects::nonNull).collect(Collectors.toList()),
                        instanceMetaDataV4Response -> InstanceStatus.CREATED));
    }

}
