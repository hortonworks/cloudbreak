package com.sequenceiq.periscope.utils;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType.GATEWAY_PRIMARY;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_HEALTHY;
import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.util.HashSet;
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
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.DependentHostGroupsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

@Service
public class StackResponseUtils {

    public Optional<InstanceMetaDataV4Response> getNotTerminatedPrimaryGateways(StackV4Response stackResponse) {
        return stackResponse.getInstanceGroups().stream().flatMap(ig -> ig.getMetadata().stream()).filter(
                im -> im.getInstanceType() == GATEWAY_PRIMARY
                        && im.getInstanceStatus() != InstanceStatus.TERMINATED
        ).findFirst();
    }

    public boolean primaryGatewayHealthy(StackV4Response stackResponse) {
        return stackResponse.getInstanceGroups().stream().flatMap(ig -> ig.getMetadata().stream())
                .anyMatch(im -> GATEWAY_PRIMARY.equals(im.getInstanceType()) && SERVICES_HEALTHY.equals(im.getInstanceStatus()));
    }

    public Map<String, String> getCloudInstanceIdsForHostGroup(StackV4Response stackResponse, String hostGroup) {
        return stackResponse.getInstanceGroups().stream()
                .filter(instanceGroupV4Response -> instanceGroupV4Response.getName().equalsIgnoreCase(hostGroup))
                .flatMap(instanceGroupV4Response -> instanceGroupV4Response.getMetadata().stream())
                .filter(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() != null)
                .collect(Collectors.toMap(InstanceMetaDataV4Response::getDiscoveryFQDN,
                        InstanceMetaDataV4Response::getInstanceId));
    }

    public List<String> getCloudInstanceIdsWithServicesHealthyForHostGroup(StackV4Response stackResponse, String hostGroup) {
        return stackResponse.getInstanceGroups().stream()
                .filter(instanceGroupV4Response -> instanceGroupV4Response.getName().equalsIgnoreCase(hostGroup))
                .flatMap(instanceGroupV4Response -> instanceGroupV4Response.getMetadata().stream())
                .filter(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() != null)
                .filter(instanceMetaData -> SERVICES_HEALTHY.equals(instanceMetaData.getInstanceStatus()))
                .map(InstanceMetaDataV4Response::getInstanceId)
                .collect(Collectors.toList());
    }

    public List<String> getStoppedCloudInstanceIdsInHostGroup(StackV4Response stackV4Response, String policyHostGroup) {
        return stackV4Response.getInstanceGroups().stream()
                .filter(instanceGroupV4Response -> instanceGroupV4Response.getName().equalsIgnoreCase(policyHostGroup))
                .flatMap(instanceGroupV4Response -> instanceGroupV4Response.getMetadata().stream())
                .filter(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() != null)
                .filter(instanceMetaData -> InstanceStatus.STOPPED.equals(instanceMetaData.getInstanceStatus()))
                .map(InstanceMetaDataV4Response::getInstanceId)
                .collect(Collectors.toList());
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

    public Set<String> getRoleTypesOnHostGroup(String template, String hostGroupName) {
        ApiClusterTemplate cmTemplate;
        try {
            cmTemplate = JsonUtil.readValue(template, ApiClusterTemplate.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Set<String> hostGroupRoleConfigNames = new HashSet<>(cmTemplate.getHostTemplates().stream()
                .filter(clusterTemplate -> clusterTemplate.getRefName().equalsIgnoreCase(hostGroupName))
                .findFirst().map(ApiClusterTemplateHostTemplate::getRoleConfigGroupsRefNames).orElse(List.of()));

        Set<String> servicesOnHostGroup = new HashSet<>();
        for (ApiClusterTemplateService apiClusterTemplateService : cmTemplate.getServices()) {
            for (ApiClusterTemplateRoleConfigGroup apiClusterTemplateRoleConfigGroup : apiClusterTemplateService.getRoleConfigGroups()) {
                if (hostGroupRoleConfigNames.contains(apiClusterTemplateRoleConfigGroup.getRefName())) {
                    servicesOnHostGroup.add(apiClusterTemplateRoleConfigGroup.getRoleType());
                }
            }
        }
        return servicesOnHostGroup;
    }

    public Set<String> getUnhealthyDependentHosts(StackV4Response stackResponse, DependentHostGroupsV4Response dependentHostGroupsResponse,
            String policyHostGroup) {
        return stackResponse.getInstanceGroups().stream()
                .flatMap(ig -> ig.getMetadata().stream())
                .filter(im -> dependentHostGroupsResponse.getDependentHostGroups().getOrDefault(policyHostGroup, Set.of())
                        .contains(im.getInstanceGroup()))
                .filter(im -> !InstanceStatus.SERVICES_HEALTHY.equals(im.getInstanceStatus()))
                .map(InstanceMetaDataV4Response::getDiscoveryFQDN)
                .collect(toSet());
    }
}
