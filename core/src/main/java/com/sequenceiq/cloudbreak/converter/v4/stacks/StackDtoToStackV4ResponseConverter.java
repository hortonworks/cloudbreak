package com.sequenceiq.cloudbreak.converter.v4.stacks;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.StackDto;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.topology.GatewayTopologyV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.loadbalancer.LoadBalancerResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.tags.TagsV4Response;
import com.sequenceiq.cloudbreak.cloud.model.CloudbreakDetails;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.converter.v4.recipes.RecipeToRecipeV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.authentication.StackAuthenticationToStackAuthenticationV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.ClusterDtoToClusterV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.customdomains.StackToCustomDomainsSettingsV4Response;
import com.sequenceiq.cloudbreak.converter.v4.stacks.database.ExternalDatabaseToDatabaseResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.InstanceGroupToInstanceGroupV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.loadbalancer.LoadBalancerToLoadBalancerResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.network.NetworkToNetworkV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.workspaces.WorkspaceToWorkspaceResourceV4ResponseConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.ServiceEndpointCollector;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.sharedservice.DatalakeService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupDto;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetadataDto;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.cloudbreak.service.stack.StackProxy;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;

@Component
public class StackDtoToStackV4ResponseConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackDtoToStackV4ResponseConverter.class);

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
    private ClusterDtoToClusterV4ResponseConverter clusterDtoToClusterV4ResponseConverter;

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

    public StackV4Response convert(StackProxy stackProxy) {
        StackDto source = stackProxy.getStack();
        StackV4Response response = new StackV4Response();
        try {
            restRequestThreadLocalService.setRequestedWorkspaceId(stackProxy.getWorkspace().getId());

            Image image = imageService.getImage(source.getId());
            response.setImage(imageToStackImageV4ResponseConverter.convert(image));
        } catch (CloudbreakImageNotFoundException exc) {
            LOGGER.debug(exc.getMessage());
        }

        response.setName(source.getName());
        response.setAuthentication(stackAuthenticationToStackAuthenticationV4ResponseConverter.convert(source));
        response.setCrn(source.getResourceCrn());
        response.setId(source.getId());
        response.setEnvironmentCrn(source.getEnvironmentCrn());
        response.setCloudPlatform(CloudPlatform.valueOf(source.getCloudPlatform()));
        response.setPlacement(stackToPlacementSettingsV4ResponseConverter.convert(source));
        response.setStatus(source.getStackStatus());
        response.setTerminated(source.getTerminated());
        response.setStatusReason(source.getStatusReason());
        response.setInstanceGroups(getInstanceGroups(stackProxy));
        response.setTunnel(source.getTunnel());
        response.setCluster(clusterDtoToClusterV4ResponseConverter.convert(stackProxy));
        response.setNetwork(networkToNetworkV4ResponseConverter.convert(stackProxy));
//        providerParameterCalculator.parse(new HashMap<>(source.getParameters()), response);
        response.setCreated(source.getCreated());
        response.setGatewayPort(source.getGatewayPort());
        response.setCustomDomains(stackToCustomDomainsSettingsV4Response.convert(source));
        response.setWorkspace(workspaceToWorkspaceResourceV4ResponseConverter.convert(stackProxy.getWorkspace()));
        addNodeCount(response);
        convertComponentConfig(response, source);
        convertTelemetryComponent(response, source);
        response.setTags(getTags(source.getTags()));
        response.setTimeToLive(getStackTimeToLive(source));
        response.setVariant(Strings.isNullOrEmpty(source.getPlatformVariant()) ? source.getCloudPlatform() : source.getPlatformVariant());
        response.setExternalDatabase(externalDatabaseToDatabaseResponseConverter
                .convert(source.getExternalDatabaseCreationType(), source.getExternalDatabaseEngineVersion()));
        datalakeService.addSharedServiceResponse(source.getDatalakeCrn(), response);
        filterExposedServicesByType(source.getType(), response.getCluster());
        response.setLoadBalancers(convertLoadBalancers(source.getId()));
        return response;
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

    private void convertTelemetryComponent(StackV4Response response, StackDto source) {
        TelemetryResponse telemetryResponse = null;
        try {
            Telemetry telemetry = componentConfigProviderService.getTelemetry(source.getId());
            telemetryResponse = telemetryConverter.convert(telemetry);
        } catch (CloudbreakServiceException exc) {
            LOGGER.debug(exc.getMessage());
        }
        response.setTelemetry(telemetryResponse);
    }

    private void convertComponentConfig(StackV4Response stackV4Response, StackDto source) {
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

    private List<InstanceGroupV4Response> getInstanceGroups(StackProxy stackProxy) {
        var instanceGroups = new LinkedList<InstanceGroupV4Response>();
        for (Map.Entry<InstanceGroupDto, List<InstanceMetadataDto>> instanceGroup : stackProxy.getInstanceGroups().entrySet()) {
            var instanceGroupResponse = instanceGroupToInstanceGroupV4ResponseConverter.convert(instanceGroup.getKey(), instanceGroup.getValue());
            collectInformationsFromActualHostgroup(stackProxy.getCluster().getId(), instanceGroup.getKey(), instanceGroupResponse);
            instanceGroupResponse.setRecoveryMode(hostGroupService.getRecoveryMode(stackProxy.getCluster().getId(), instanceGroup.getKey().getGroupName()));
            instanceGroups.add(instanceGroupResponse);
        }
        return instanceGroups;
    }

    private void collectInformationsFromActualHostgroup(Long clusterId, InstanceGroupDto instanceGroup, InstanceGroupV4Response instanceGroupResponse) {
        List<Recipe> recipes = hostGroupService.getRecipesForHostGroup(clusterId, instanceGroup.getGroupName());
        instanceGroupResponse.setRecipes(
                recipes.stream()
                        .map(e -> recipeToRecipeV4ResponseConverter.convert(e))
                        .collect(Collectors.toList())
        );

    }

    private Long getStackTimeToLive(StackDto stack) {
//        Map<String, String> params = stack.getParameters();
//        Optional<String> optional = Optional.ofNullable(params.get(PlatformParametersConsts.TTL_MILLIS));
//        if (optional.isPresent()) {
//            return optional.map(Long::parseLong).get();
//        }
        return null;
    }

    private List<LoadBalancerResponse> convertLoadBalancers(Long stackId) {
        Set<LoadBalancer> loadBalancers = loadBalancerService.findByStackId(stackId);
        return loadBalancers.isEmpty() ? null : loadBalancers.stream()
                .map(l -> loadBalancerToLoadBalancerResponseConverter.convert(l))
                .collect(Collectors.toList());
    }

}
