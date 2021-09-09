package com.sequenceiq.cloudbreak.converter.v4.stacks;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.topology.GatewayTopologyV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.loadbalancer.LoadBalancerResponse;
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
import com.sequenceiq.cloudbreak.converter.v4.stacks.database.DatabaseAvailabilityTypeToDatabaseResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.InstanceGroupToInstanceGroupV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.network.InstanceGroupNetworkToInstanceGroupNetworkV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.loadbalancer.LoadBalancerToLoadBalancerResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.network.NetworkToNetworkV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.workspaces.WorkspaceToWorkspaceResourceV4ResponseConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.ServiceEndpointCollector;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.sharedservice.DatalakeService;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
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
    private DatabaseAvailabilityTypeToDatabaseResponseConverter databaseAvailabilityTypeToDatabaseResponseConverter;

    @Inject
    private RecipeToRecipeV4ResponseConverter recipeToRecipeV4ResponseConverter;

    @Inject
    private LoadBalancerToLoadBalancerResponseConverter loadBalancerToLoadBalancerResponseConverter;

    @Inject
    private InstanceGroupNetworkToInstanceGroupNetworkV4ResponseConverter instanceGroupNetworkToInstanceGroupNetworkV4ResponseConverter;

    public StackV4Response convert(Stack source) {
        StackV4Response response = new StackV4Response();
        try {
            restRequestThreadLocalService.setWorkspace(source.getWorkspace());

            Image image = imageService.getImage(source.getId());
            response.setImage(imageToStackImageV4ResponseConverter.convert(image));
        } catch (CloudbreakImageNotFoundException exc) {
            LOGGER.debug(exc.getMessage());
        }

        response.setName(source.getName());
        response.setAuthentication(stackAuthenticationToStackAuthenticationV4ResponseConverter
                .convert(source.getStackAuthentication()));
        response.setCrn(source.getResourceCrn());
        response.setId(source.getId());
        response.setEnvironmentCrn(source.getEnvironmentCrn());
        response.setCloudPlatform(CloudPlatform.valueOf(source.getCloudPlatform()));
        response.setPlacement(stackToPlacementSettingsV4ResponseConverter.convert(source));
        response.setStatus(source.getStatus());
        response.setTerminated(source.getTerminated());
        response.setStatusReason(source.getStatusReason());
        response.setInstanceGroups(getInstanceGroups(source));
        response.setTunnel(source.getTunnel());
        response.setCluster(clusterToClusterV4ResponseConverter.convert(source.getCluster()));
        response.setNetwork(networkToNetworkV4ResponseConverter.convert(source));
        providerParameterCalculator.parse(new HashMap<>(source.getParameters()), response);
        response.setCreated(source.getCreated());
        response.setGatewayPort(source.getGatewayPort());
        response.setCustomDomains(stackToCustomDomainsSettingsV4Response.convert(source));
        response.setWorkspace(workspaceToWorkspaceResourceV4ResponseConverter
                .convert(source.getWorkspace()));
        addNodeCount(source, response);
        convertComponentConfig(response, source);
        convertTelemetryComponent(response, source);
        response.setTags(getTags(source.getTags()));
        response.setTimeToLive(getStackTimeToLive(source));
        response.setVariant(Strings.isNullOrEmpty(source.getPlatformVariant()) ? source.getCloudPlatform() : source.getPlatformVariant());
        response.setExternalDatabase(getIfNotNull(source.getExternalDatabaseCreationType(),
                ed -> databaseAvailabilityTypeToDatabaseResponseConverter.convert(ed)));
        datalakeService.addSharedServiceResponse(source, response);
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

    private void addNodeCount(Stack source, StackV4Response stackJson) {
        int nodeCount = 0;
        for (InstanceGroup instanceGroup : source.getInstanceGroups()) {
            nodeCount += instanceGroup.getNodeCount();
        }
        stackJson.setNodeCount(nodeCount);
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

    private void convertTelemetryComponent(StackV4Response response, Stack source) {
        TelemetryResponse telemetryResponse = null;
        try {
            Telemetry telemetry = componentConfigProviderService.getTelemetry(source.getId());
            telemetryResponse = telemetryConverter.convert(telemetry);
        } catch (CloudbreakServiceException exc) {
            LOGGER.debug(exc.getMessage());
        }
        response.setTelemetry(telemetryResponse);
    }

    private void convertComponentConfig(StackV4Response stackV4Response, Stack source) {
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

    private List<InstanceGroupV4Response> getInstanceGroups(Stack stack) {
        var instanceGroups = new LinkedList<InstanceGroupV4Response>();
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            var instanceGroupResponse = instanceGroupToInstanceGroupV4ResponseConverter
                    .convert(instanceGroup);
            collectInformationsFromActualHostgroup(stack.getCluster(), instanceGroup, instanceGroupResponse);
            instanceGroups.add(instanceGroupResponse);
        }
        return instanceGroups;
    }

    private void collectInformationsFromActualHostgroup(Cluster cluster, InstanceGroup instanceGroup, InstanceGroupV4Response instanceGroupResponse) {
        if (cluster != null && cluster.getHostGroups() != null) {
            cluster.getHostGroups().stream()
                    .filter(hostGroup -> hostGroup.getName().equals(instanceGroup.getGroupName()))
                    .findFirst()
                    .ifPresent(hostGroup -> {
                        instanceGroupResponse.setRecipes(
                                hostGroup.getRecipes().stream()
                                        .map(e -> recipeToRecipeV4ResponseConverter.convert(e))
                                        .collect(Collectors.toList())
                        );
                        instanceGroupResponse.setRecoveryMode(hostGroup.getRecoveryMode());
                        instanceGroupResponse.setAvailabilityZones(instanceGroup.getAvailabilityZones());
                        instanceGroupResponse.setNetwork(instanceGroupNetworkToInstanceGroupNetworkV4ResponseConverter
                                .convert(instanceGroup.getInstanceGroupNetwork()));
                        instanceGroupResponse.getMetadata().stream()
                                .filter(md -> md.getDiscoveryFQDN() != null)
                                .forEach(md -> {
                                    instanceGroup.getInstanceMetaDataSet().stream()
                                            .filter(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() != null
                                                    && instanceMetaData.getDiscoveryFQDN().equals(md.getDiscoveryFQDN()))
                                            .findFirst()
                                            .ifPresent(instanceMetaData -> {
                                                md.setState(instanceMetaData.getInstanceStatus().getAsHostState());
                                                md.setStatusReason(instanceMetaData.getStatusReason());
                                            });
                                });
                    });
        }
    }

    private Long getStackTimeToLive(Stack stack) {
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
