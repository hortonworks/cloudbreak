package com.sequenceiq.cloudbreak.converter.v4.stacks;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.CloudbreakDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.PlacementSettingsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.authentication.StackAuthenticationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.topology.GatewayTopologyV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.sharedservice.AttachedClusterInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.sharedservice.SharedServiceV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.customdomain.CustomDomainSettingsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.StackImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.network.InstanceGroupNetworkV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.network.NetworkV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.tags.TagsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.model.CloudbreakDetails;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.projection.StackIdView;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.ServiceEndpointCollector;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeResourcesService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;

@Component
public class StackToStackV4ResponseConverter extends AbstractConversionServiceAwareConverter<Stack, StackV4Response> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackToStackV4ResponseConverter.class);

    @Inject
    private ImageService imageService;

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private ProviderParameterCalculator providerParameterCalculator;

    @Inject
    private TelemetryConverter telemetryConverter;

    @Inject
    private DatalakeResourcesService datalakeResourcesService;

    @Inject
    private StackService stackService;

    @Inject
    private ServiceEndpointCollector serviceEndpointCollector;

    @Override
    public StackV4Response convert(Stack source) {
        StackV4Response response = new StackV4Response();
        try {
            Image image = imageService.getImage(source.getId());
            response.setImage(getConversionService().convert(image, StackImageV4Response.class));
        } catch (CloudbreakImageNotFoundException exc) {
            LOGGER.debug(exc.getMessage());
        }

        response.setName(source.getName());
        response.setAuthentication(getConversionService().convert(source.getStackAuthentication(), StackAuthenticationV4Response.class));
        response.setCrn(source.getResourceCrn());
        response.setId(source.getId());
        response.setEnvironmentCrn(source.getEnvironmentCrn());
        response.setCloudPlatform(CloudPlatform.valueOf(source.getCloudPlatform()));
        response.setPlacement(getConversionService().convert(source, PlacementSettingsV4Response.class));
        response.setStatus(source.getStatus());
        response.setTerminated(source.getTerminated());
        response.setStatusReason(source.getStatusReason());
        response.setInstanceGroups(getInstanceGroups(source));
        response.setTunnel(source.getTunnel());
        response.setCluster(getConversionService().convert(source.getCluster(), ClusterV4Response.class));
        response.setNetwork(getConversionService().convert(source, NetworkV4Response.class));
        providerParameterCalculator.parse(new HashMap<>(source.getParameters()), response);
        response.setCreated(source.getCreated());
        response.setGatewayPort(source.getGatewayPort());
        response.setCustomDomains(getConversionService().convert(source, CustomDomainSettingsV4Response.class));
        response.setWorkspace(getConversionService().convert(source.getWorkspace(), WorkspaceResourceV4Response.class));
        addNodeCount(source, response);
        convertComponentConfig(response, source);
        convertTelemetryComponent(response, source);
        response.setTags(getTags(source.getTags()));
        response.setTimeToLive(getStackTimeToLive(source));
        response.setExternalDatabase(getIfNotNull(source.getExternalDatabaseCreationType(),
                ed -> getConversionService().convert(ed, DatabaseResponse.class)));
        addSharedServiceResponse(source, response);
        filterExposedServicesByType(source.getType(), response.getCluster());
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
                return getConversionService().convert(stackTag, TagsV4Response.class);
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
                stackV4Response.setCloudbreakDetails(getConversionService().convert(cloudbreakDetails, CloudbreakDetailsV4Response.class));
            }
        } catch (RuntimeException e) {
            LOGGER.info("Failed to convert dynamic component.", e);
        }

    }

    private List<InstanceGroupV4Response> getInstanceGroups(Stack stack) {
        var instanceGroups = new LinkedList<InstanceGroupV4Response>();
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            var instanceGroupResponse = getConversionService().convert(instanceGroup, InstanceGroupV4Response.class);
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
                        instanceGroupResponse.setRecipes(converterUtil.convertAll(hostGroup.getRecipes(), RecipeV4Response.class));
                        instanceGroupResponse.setRecoveryMode(hostGroup.getRecoveryMode());
                        instanceGroupResponse.setNetwork(converterUtil.convert(instanceGroup.getInstanceGroupNetwork(), InstanceGroupNetworkV4Response.class));
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

    private void addSharedServiceResponse(Stack stack, StackV4Response stackResponse) {
        SharedServiceV4Response sharedServiceResponse = new SharedServiceV4Response();
        LOGGER.debug("Checking whether the datalake resource id is null or not for the cluster: " + stack.getResourceCrn());
        if (stack.getDatalakeResourceId() != null) {
            Optional<DatalakeResources> datalakeResources = datalakeResourcesService.findById(stack.getDatalakeResourceId());
            if (datalakeResources.isPresent()) {
                DatalakeResources datalakeResource = datalakeResources.get();
                sharedServiceResponse.setSharedClusterId(datalakeResource.getDatalakeStackId());
                sharedServiceResponse.setSharedClusterName(datalakeResource.getName());
                LOGGER.debug("DatalakeResources (datalake stack id: {} ,name: {}) has found for stack: {}",
                        datalakeResource.getDatalakeStackId(), datalakeResource.getName(), stack.getResourceCrn());
            } else {
                LOGGER.debug("Unable to find DatalakeResources for datalake resource id: " + stack.getDatalakeResourceId());
            }
        } else {
            LOGGER.debug("Datalake resource ID was null! Going to search it by the given stack's id (id: {})", stack.getId());
            Optional<DatalakeResources> datalakeResources = datalakeResourcesService.findByDatalakeStackId(stack.getId());
            if (datalakeResources.isPresent()) {
                LOGGER.debug("DatalakeResources (datalake stack id: {} ,name: {}) has found for stack: {}",
                        datalakeResources.get().getDatalakeStackId(), datalakeResources.get().getName(), stack.getResourceCrn());
                for (StackIdView connectedStacks : stackService.findClustersConnectedToDatalakeByDatalakeResourceId(datalakeResources.get().getId())) {
                    AttachedClusterInfoV4Response attachedClusterInfoResponse = new AttachedClusterInfoV4Response();
                    attachedClusterInfoResponse.setId(connectedStacks.getId());
                    attachedClusterInfoResponse.setName(connectedStacks.getName());
                    sharedServiceResponse.getAttachedClusters().add(attachedClusterInfoResponse);
                }
            } else {
                LOGGER.debug("Unable to find DatalakeResources by the stack's id: {}", stack.getId());
            }
        }
        stackResponse.setSharedService(sharedServiceResponse);
    }

}
