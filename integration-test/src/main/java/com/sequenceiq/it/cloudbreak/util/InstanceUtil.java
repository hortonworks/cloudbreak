package com.sequenceiq.it.cloudbreak.util;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
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

    public static Set<String> getInstancePrivateIps(List<InstanceGroupV4Response> instanceGroupV4Responses, String hostGroupName) {
        InstanceGroupV4Response instanceGroupV4Response = instanceGroupV4Responses.stream().filter(instanceGroup ->
                instanceGroup.getName().equals(hostGroupName)).findFirst().orElse(null);
        return Objects.requireNonNull(instanceGroupV4Response)
                .getMetadata().stream().map(InstanceMetaDataV4Response::getPrivateIp).collect(Collectors.toSet());
    }

    public static Map<String, String> getInstanceIpIdMap(List<InstanceGroupV4Response> instanceGroupV4Responses, String hostGroupName) {
        InstanceGroupV4Response instanceGroupV4Response = instanceGroupV4Responses.stream().filter(instanceGroup ->
                instanceGroup.getName().equals(hostGroupName)).findFirst().orElse(null);
        return Objects.requireNonNull(instanceGroupV4Response)
                .getMetadata().stream().collect(Collectors.toMap(InstanceMetaDataV4Response::getInstanceId, InstanceMetaDataV4Response::getPrivateIp));
    }

    public static Map<String, String> getInstancesWithAz(List<InstanceGroupV4Response> instanceGroupV4Responses, String hostGroupName) {
        InstanceGroupV4Response instanceGroupV4Response = instanceGroupV4Responses.stream().filter(instanceGroup ->
                instanceGroup.getName().equals(hostGroupName)).findFirst().orElse(null);
        return Objects.requireNonNull(instanceGroupV4Response)
                .getMetadata().stream().collect(Collectors.toMap(InstanceMetaDataV4Response::getInstanceId, InstanceMetaDataV4Response::getAvailabilityZone));
    }

    public static Map<List<String>, InstanceStatus> getInstanceStatusMapForStatus(StackV4Response stackV4Response, InstanceStatus status) {
        return stackV4Response.getInstanceGroups().stream()
                .filter(instanceGroupV4Response -> instanceGroupV4Response.getMetadata().stream()
                        .anyMatch(instanceMetaDataV4Response -> Objects.nonNull(instanceMetaDataV4Response.getInstanceId())))
                .collect(Collectors.toMap(
                        instanceGroupV4Response -> instanceGroupV4Response.getMetadata().stream()
                                .filter(instanceMetaDataV4Response -> Objects.nonNull(instanceMetaDataV4Response.getInstanceId()))
                                .map(InstanceMetaDataV4Response::getInstanceId).collect(Collectors.toList()),
                        instanceMetaDataV4Response -> status));
    }
}
