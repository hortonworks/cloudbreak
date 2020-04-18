package com.sequenceiq.periscope.utils;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.InstanceTemplateV4Response;

@Service
public class StackResponseUtils {

    public Optional<InstanceMetaDataV4Response> getNotTerminatedPrimaryGateways(StackV4Response stackResponse) {
        return stackResponse.getInstanceGroups().stream().flatMap(ig -> ig.getMetadata().stream()).filter(
                im -> im.getInstanceType() == InstanceMetadataType.GATEWAY_PRIMARY
                        && im.getInstanceStatus() != InstanceStatus.TERMINATED
        ).findFirst();
    }

    public String getHostGroupInstanceType(StackV4Response stackResponse, String hostGroup) {
        return stackResponse.getInstanceGroups().stream()
                .filter(instanceGroupV4Response -> instanceGroupV4Response.getName().equalsIgnoreCase(hostGroup))
                .findFirst()
                .map(InstanceGroupV4Response::getTemplate)
                .map(InstanceTemplateV4Response::getInstanceType)
                .orElseThrow(() -> new RuntimeException(
                        String.format("HostGroup %s InstanceTemplateV4Response not found for cluster %s.", hostGroup, stackResponse.getCrn())));
    }

    public Map<String, String> getCloudInstanceIdsForHostGroup(StackV4Response stackResponse, String hostGroup) {
        return stackResponse.getInstanceGroups().stream()
                .filter(instanceGroupV4Response -> instanceGroupV4Response.getName().equalsIgnoreCase(hostGroup))
                .flatMap(instanceGroupV4Response -> instanceGroupV4Response.getMetadata().stream())
                .collect(Collectors.toMap(InstanceMetaDataV4Response::getDiscoveryFQDN,
                        InstanceMetaDataV4Response::getInstanceId));
    }

    public Integer getNodeCountForHostGroup(StackV4Response stackResponse, String hostGroup) {
        return stackResponse.getInstanceGroups().stream()
                .filter(instanceGroupV4Response -> instanceGroupV4Response.getName().equalsIgnoreCase(hostGroup))
                .flatMap(instanceGroupV4Response -> instanceGroupV4Response.getMetadata().stream())
                .map(InstanceMetaDataV4Response::getDiscoveryFQDN)
                .collect(Collectors.counting())
                .intValue();
    }
}
