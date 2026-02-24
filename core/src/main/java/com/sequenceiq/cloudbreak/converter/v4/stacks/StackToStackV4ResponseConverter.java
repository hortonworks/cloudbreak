package com.sequenceiq.cloudbreak.converter.v4.stacks;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.SecurityV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.topology.GatewayTopologyV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.loadbalancer.LoadBalancerResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.resource.ResourceV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.tags.TagsV4Response;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.model.CloudbreakDetails;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.converter.v4.recipes.RecipeToRecipeV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.authentication.StackAuthenticationToStackAuthenticationV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.ClusterToClusterV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.customdomains.StackToCustomDomainsSettingsV4Response;
import com.sequenceiq.cloudbreak.converter.v4.stacks.database.ExternalDatabaseToDatabaseResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.InstanceGroupToInstanceGroupV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.loadbalancer.LoadBalancerToLoadBalancerResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.network.NetworkToNetworkV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.workspaces.WorkspaceToWorkspaceResourceV4ResponseConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.ServiceEndpointCollector;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.sharedservice.DatalakeService;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;

@Component
public class StackToStackV4ResponseConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackToStackV4ResponseConverter.class);

    @Inject
    private ImageService imageService;

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private ProviderParameterCalculator providerParameterCalculator;

    @Inject
    private TelemetryConverter telemetryConverter;

    @Inject
    private ServiceEndpointCollector serviceEndpointCollector;

    @Inject
    private DatalakeService datalakeService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private LoadBalancerPersistenceService loadBalancerService;

    @Inject
    private StackToPlacementSettingsV4ResponseConverter stackToPlacementSettingsV4ResponseConverter;

    @Inject
    private ImageToStackImageV4ResponseConverter imageToStackImageV4ResponseConverter;

    @Inject
    private StackTagsToTagsV4ResponseConverter stackTagsToTagsV4ResponseConverter;

    @Inject
    private ClusterToClusterV4ResponseConverter clusterToClusterV4ResponseConverter;

    @Inject
    private NetworkToNetworkV4ResponseConverter networkToNetworkV4ResponseConverter;

    @Inject
    private WorkspaceToWorkspaceResourceV4ResponseConverter workspaceToWorkspaceResourceV4ResponseConverter;

    @Inject
    private StackToCustomDomainsSettingsV4Response stackToCustomDomainsSettingsV4Response;

    @Inject
    private InstanceGroupToInstanceGroupV4ResponseConverter instanceGroupToInstanceGroupV4ResponseConverter;

    @Inject
    private CloudbreakDetailsToCloudbreakDetailsV4ResponseConverter cloudbreakDetailsToCloudbreakDetailsV4ResponseConverter;

    @Inject
    private StackAuthenticationToStackAuthenticationV4ResponseConverter stackAuthenticationToStackAuthenticationV4ResponseConverter;

    @Inject
    private RecipeToRecipeV4ResponseConverter recipeToRecipeV4ResponseConverter;

    @Inject
    private LoadBalancerToLoadBalancerResponseConverter loadBalancerToLoadBalancerResponseConverter;

    @Inject
    private ExternalDatabaseToDatabaseResponseConverter externalDatabaseToDatabaseResponseConverter;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private ResourceToResourceV4ResponseConverter resourceToResourceV4ResponseConverter;

    public StackV4Response convert(StackDtoDelegate stack) {
        StackView source = stack.getStack();
        StackV4Response response = new StackV4Response();
        try {
            restRequestThreadLocalService.setWorkspaceId(source.getWorkspaceId());

            Image image = imageService.getImage(source.getId());
            response.setImage(imageToStackImageV4ResponseConverter.convert(image));
            response.setResources(convertResources(stack.getResources()));
        } catch (CloudbreakImageNotFoundException | NotImplementedException exc) {
            LOGGER.debug(exc.getMessage());
        }
        response.setName(source.getName());
        response.setAuthentication(stackAuthenticationToStackAuthenticationV4ResponseConverter.convert(source.getStackAuthentication()));
        response.setCrn(source.getResourceCrn());
        response.setId(source.getId());
        response.setEnvironmentCrn(source.getEnvironmentCrn());
        response.setCloudPlatform(CloudPlatform.valueOf(source.getCloudPlatform()));
        response.setPlacement(stackToPlacementSettingsV4ResponseConverter.convert(source));
        response.setStatus(source.getStatus());
        response.setTerminated(source.getTerminated());
        response.setStatusReason(source.getStatusReason());
        response.setInstanceGroups(getInstanceGroups(stack));
        response.setTunnel(source.getTunnel());
        response.setCluster(clusterToClusterV4ResponseConverter.convert(stack));
        response.setNetwork(networkToNetworkV4ResponseConverter.convert(stack));
        providerParameterCalculator.parse(new HashMap<>(stack.getParameters()), response);
        response.setCreated(source.getCreated());
        response.setGatewayPort(source.getGatewayPort());
        response.setCustomDomains(stackToCustomDomainsSettingsV4Response.convert(source));
        response.setWorkspace(workspaceToWorkspaceResourceV4ResponseConverter.convert(stack.getWorkspace()));
        addNodeCount(response);
        convertComponentConfig(response, source);
        convertTelemetryComponent(response, source);
        response.setTags(getTags(source.getTags()));
        response.setTimeToLive(getStackTimeToLive(stack));
        response.setNotificationState(source.getNotificationState());
        response.setVariant(Strings.isNullOrEmpty(source.getPlatformVariant()) ? source.getCloudPlatform() : source.getPlatformVariant());
        response.setExternalDatabase(externalDatabaseToDatabaseResponseConverter
                .convert(stack.getExternalDatabaseCreationType(), stack.getExternalDatabaseEngineVersion()));
        response.setJavaVersion(source.getJavaVersion());
        response.setEnableMultiAz(source.isMultiAz());
        datalakeService.addSharedServiceResponse(response);
        filterExposedServicesByType(source.getType(), response.getCluster());
        response.setLoadBalancers(convertLoadBalancers(source.getId()));
        if (!CollectionUtils.isEmpty(response.getLoadBalancers())) {
            response.setEnableLoadBalancer(true);
        }
        response.setSecurity(convertSecurity(stack));
        response.setSupportedImdsVersion(stack.getSupportedImdsVersion());
        response.setArchitecture(stack.getStack().getArchitecture().getName());
        response.setRegion(stack.getRegion());
        response.setProviderSyncStates(stack.getStack().getProviderSyncStates());
        return response;
    }

    private SecurityV4Response convertSecurity(StackDtoDelegate stack) {
        SecurityV4Response securityV4Response = new SecurityV4Response();
        SecurityConfig securityConfig = stack.getSecurityConfig();
        if (securityConfig != null && securityConfig.getSeLinux() != null) {
            securityV4Response.setSeLinux(securityConfig.getSeLinux().name());
        }
        return securityV4Response;
    }

    private List<ResourceV4Response> convertResources(Set<Resource> resourceList) {
        return resourceList.stream().map(resourceToResourceV4ResponseConverter::convertResourceToResourceV4Response).collect(Collectors.toList());
    }

    private void filterExposedServicesByType(StackType stackType, ClusterV4Response clusterV4Response) {
        if (clusterV4Response != null && clusterV4Response.getGateway() != null) {
            List<GatewayTopologyV4Response> gatewayTopologyV4Responses = serviceEndpointCollector
                    .filterByStackType(stackType, clusterV4Response.getGateway().getTopologies());
            clusterV4Response.getGateway().setTopologies(gatewayTopologyV4Responses);
        }
    }

    private void addNodeCount(StackV4Response response) {
        int nodeCount = 0;
        for (InstanceGroupV4Response instanceGroup : response.getInstanceGroups()) {
            nodeCount += instanceGroup.getNodeCount();
        }
        response.setNodeCount(nodeCount);
    }

    private TagsV4Response getTags(Json tag) {
        try {
            if (tag != null && tag.getValue() != null) {
                StackTags stackTag = tag.get(StackTags.class);
                return stackTagsToTagsV4ResponseConverter.convert(stackTag);
            }
        } catch (Exception e) {
            LOGGER.info("Failed to convert dynamic tags.", e);
        }
        TagsV4Response response = new TagsV4Response();
        response.setApplication(new HashMap<>());
        response.setDefaults(new HashMap<>());
        response.setUserDefined(new HashMap<>());
        return response;
    }

    private void convertTelemetryComponent(StackV4Response response, StackView source) {
        TelemetryResponse telemetryResponse = null;
        try {
            Telemetry telemetry = componentConfigProviderService.getTelemetry(source.getId());
            telemetryResponse = telemetryConverter.convert(telemetry);
        } catch (CloudbreakServiceException exc) {
            LOGGER.debug(exc.getMessage());
        }
        response.setTelemetry(telemetryResponse);
    }

    private void convertComponentConfig(StackV4Response stackV4Response, StackView source) {
        try {
            CloudbreakDetails cloudbreakDetails = componentConfigProviderService.getCloudbreakDetails(source.getId());
            if (cloudbreakDetails != null) {
                stackV4Response.setCloudbreakDetails(cloudbreakDetailsToCloudbreakDetailsV4ResponseConverter
                        .convert(cloudbreakDetails));
            }
        } catch (RuntimeException e) {
            LOGGER.info("Failed to convert dynamic component.", e);
        }

    }

    private List<InstanceGroupV4Response> getInstanceGroups(StackDtoDelegate stack) {
        List<InstanceGroupV4Response> instanceGroups = new LinkedList<>();
        for (InstanceGroupDto instanceGroupDto : stack.getInstanceGroupDtos()) {
            InstanceGroupView instanceGroup = instanceGroupDto.getInstanceGroup();
            InstanceGroupV4Response instanceGroupResponse = instanceGroupToInstanceGroupV4ResponseConverter
                    .convert(instanceGroup, stack.getAvailabilityZonesByInstanceGroup(instanceGroup.getId()), instanceGroupDto.getInstanceMetadataViews());
            collectInformationsFromActualHostgroup(stack.getCluster(), instanceGroup, instanceGroupResponse);
            instanceGroupResponse.setRecoveryMode(hostGroupService.getRecoveryMode(stack.getCluster(), instanceGroup.getGroupName()));
            instanceGroups.add(instanceGroupResponse);
        }
        return instanceGroups;
    }

    private void collectInformationsFromActualHostgroup(ClusterView cluster, InstanceGroupView instanceGroup, InstanceGroupV4Response instanceGroupResponse) {
        if (cluster != null) {
            List<Recipe> recipes = hostGroupService.getRecipesForHostGroup(cluster.getId(), instanceGroup.getGroupName());
            instanceGroupResponse.setRecipes(
                    recipes.stream()
                            .map(e -> recipeToRecipeV4ResponseConverter.convert(e))
                            .collect(Collectors.toList())
            );
        }

    }

    private Long getStackTimeToLive(StackDtoDelegate stack) {
        Map<String, String> params = stack.getParameters();
        Optional<String> optional = Optional.ofNullable(params.get(PlatformParametersConsts.TTL_MILLIS));
        if (optional.isPresent()) {
            return optional.map(Long::parseLong).get();
        }
        return null;
    }

    private List<LoadBalancerResponse> convertLoadBalancers(Long stackId) {
        Set<LoadBalancer> loadBalancers = loadBalancerService.findByStackId(stackId);
        return loadBalancers.isEmpty() ? null : loadBalancers.stream()
                .map(l -> loadBalancerToLoadBalancerResponseConverter.convert(l))
                .collect(Collectors.toList());
    }

}
