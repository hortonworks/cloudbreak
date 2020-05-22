package com.sequenceiq.periscope.utils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.model.ApiClusterTemplate;
import com.cloudera.api.swagger.model.ApiClusterTemplateHostTemplate;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

@Service
public class StackResponseUtils {

    public Optional<InstanceMetaDataV4Response> getNotTerminatedPrimaryGateways(StackV4Response stackResponse) {
        return stackResponse.getInstanceGroups().stream().flatMap(ig -> ig.getMetadata().stream()).filter(
                im -> im.getInstanceType() == InstanceMetadataType.GATEWAY_PRIMARY
                        && im.getInstanceStatus() != InstanceStatus.TERMINATED
        ).findFirst();
    }

    public Map<String, String> getCloudInstanceIdsForHostGroup(StackV4Response stackResponse, String hostGroup) {
        return stackResponse.getInstanceGroups().stream()
                .filter(instanceGroupV4Response -> instanceGroupV4Response.getName().equalsIgnoreCase(hostGroup))
                .flatMap(instanceGroupV4Response -> instanceGroupV4Response.getMetadata().stream())
                .filter(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() != null)
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

    public String getRoleConfigNameForHostGroup(StackV4Response stackResponse, String hostGroupName, String serviceType, String roleType)
            throws Exception {
        String template = stackResponse.getCluster().getBlueprint().getBlueprint();
        ApiClusterTemplate cmTemplate = JsonUtil.readValue(template, ApiClusterTemplate.class);

        Set<String> hostGroupRoleConfigNames = cmTemplate.getHostTemplates().stream()
                .filter(clusterTemplate -> clusterTemplate.getRefName().equalsIgnoreCase(hostGroupName))
                .findFirst().map(ApiClusterTemplateHostTemplate::getRoleConfigGroupsRefNames).orElse(List.of())
                .stream()
                .collect(Collectors.toSet());

        String roleReferenceName = cmTemplate.getServices().stream()
                .filter(s -> s.getServiceType().equalsIgnoreCase(serviceType))
                .findFirst()
                .map(ApiClusterTemplateService::getRoleConfigGroups).orElse(List.of())
                .stream()
                .filter(rcg -> rcg.getRoleType().equalsIgnoreCase(roleType))
                .filter(rcg -> hostGroupRoleConfigNames.contains(rcg.getRefName()))
                .map(ApiClusterTemplateRoleConfigGroup::getRefName)
                .findFirst()
                .orElseThrow(
                        () -> new Exception(String.format("Unable to retrieve RoleConfigGroupRefName for Service '%s', RoleType '%s'," +
                                " HostGroup '%s', Cluster '%s'", serviceType, roleType, hostGroupName, stackResponse.getCrn())));

        return roleReferenceName;
    }
}
