package com.sequenceiq.cloudbreak.converter.v4.stacks;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.api.endpoint.v4.flexsubscription.responses.FlexSubscriptionV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.CloudbreakDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.authentication.StackAuthenticationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.sharedservice.AttachedClusterInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.sharedservice.SharedServiceV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.customdomain.CustomDomainSettingsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.environment.EnvironmentSettingsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.StackImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.network.NetworkV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.tags.TagsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.model.CloudbreakDetails;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.service.ComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeResourcesService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class StackToStackV4ResponseConverter extends AbstractConversionServiceAwareConverter<Stack, StackV4Response> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackToStackV4ResponseConverter.class);

    @Inject
    private ImageService imageService;

    @Inject
    private ComponentConfigProvider componentConfigProvider;

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private ProviderParameterCalculator providerParameterCalculator;

    @Inject
    private DatalakeResourcesService datalakeResourcesService;

    @Inject
    private StackService stackService;

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
        response.setId(source.getId());
        if (source.getEnvironment() != null) {
            response.setEnvironment(getConversionService().convert(source, EnvironmentSettingsV4Response.class));
            response.setCloudPlatform(CloudPlatform.valueOf(source.getCloudPlatform()));
        }
        response.setStatus(source.getStatus());
        response.setTerminated(source.getTerminated());
        response.setStatusReason(source.getStatusReason());
        response.setInstanceGroups(converterUtil.convertAll(source.getInstanceGroups(), InstanceGroupV4Response.class));
        response.setCluster(getConversionService().convert(source.getCluster(), ClusterV4Response.class));
        response.setNetwork(getConversionService().convert(source.getNetwork(), NetworkV4Response.class));
        providerParameterCalculator.parse(new HashMap<>(source.getParameters()), response);
        response.setCreated(source.getCreated());
        response.setGatewayPort(source.getGatewayPort());
        response.setCustomDomains(getConversionService().convert(source, CustomDomainSettingsV4Response.class));
        response.setWorkspace(getConversionService().convert(source.getWorkspace(), WorkspaceResourceV4Response.class));
        addNodeCount(source, response);
        convertComponentConfig(response, source);
        response.setTags(getTags(response, source.getTags()));
        addFlexSubscription(response, source);
        response.setTimeToLive(getStackTimeToLive(source));
        addSharedServiceResponse(source, response);
        return response;
    }

    private void addNodeCount(Stack source, StackV4Response stackJson) {
        int nodeCount = 0;
        for (InstanceGroup instanceGroup : source.getInstanceGroups()) {
            nodeCount += instanceGroup.getNodeCount();
        }
        stackJson.setNodeCount(nodeCount);
    }

    private TagsV4Response getTags(StackV4Response stackV4Response, Json tag) {
        try {
            if (tag != null && tag.getValue() != null) {
                StackTags stackTag = tag.get(StackTags.class);
                return getConversionService().convert(stackTag, TagsV4Response.class);
            }
        } catch (Exception e) {
            LOGGER.info("Failed to convert dynamic tags.", e);
        }
        TagsV4Response response = new TagsV4Response();
        response.setApplicationTags(new HashMap<>());
        response.setDefaultTags(new HashMap<>());
        response.setUserDefinedTags(new HashMap<>());
        return response;
    }

    private void convertComponentConfig(StackV4Response stackV4Response, Stack source) {
        try {
            CloudbreakDetails cloudbreakDetails = componentConfigProvider.getCloudbreakDetails(source.getId());
            if (cloudbreakDetails != null) {
                stackV4Response.setCloudbreakDetails(getConversionService().convert(cloudbreakDetails, CloudbreakDetailsV4Response.class));
            }
        } catch (RuntimeException e) {
            LOGGER.info("Failed to convert dynamic component.", e);
        }

    }

    private void addFlexSubscription(StackV4Response stackV4Response, Stack source) {
        if (source.getFlexSubscription() != null) {
            try {
                FlexSubscriptionV4Response flexSubscription = getConversionService().convert(source.getFlexSubscription(), FlexSubscriptionV4Response.class);
                stackV4Response.setFlexSubscription(flexSubscription);
            } catch (Exception ex) {
                LOGGER.warn("Flex subscription could not be added to stack response.", ex);
            }
        }
    }

    private void collectInformationsFromActualHostgroup(Cluster cluster, InstanceGroup instanceGroup, InstanceGroupV4Response instanceGroupResponse) {
        if (cluster != null && cluster.getHostGroups() != null) {
            cluster.getHostGroups().stream()
                    .filter(hostGroup -> hostGroup.getName().equals(instanceGroup.getGroupName()))
                    .findFirst()
                    .ifPresent(hostGroup -> {
                        instanceGroupResponse.setRecipes(converterUtil.convertAll(hostGroup.getRecipes(), RecipeV4Response.class));
                        instanceGroupResponse.setRecoveryMode(hostGroup.getRecoveryMode());

                        Map<String, String> metaDataStates = new HashMap<>();

                        instanceGroup.getInstanceMetaDataSet()
                                .forEach(imd -> hostGroup.getHostMetadata().stream()
                                        .filter(s -> s.getHostName().equals(imd.getDiscoveryFQDN()))
                                        .findFirst()
                                        .ifPresent(hmd -> metaDataStates.put(hmd.getHostName(), hmd.getHostMetadataState().name()))
                                );

                        instanceGroupResponse.getMetadata().forEach(md -> md.setState(metaDataStates.get(md.getDiscoveryFQDN())));
                    });
        }
    }

    private Long getStackTimeToLive(Stack stack) {
        Map<String, String> params = stack.getParameters();
        Optional<String> optional = Optional.ofNullable(params.get(PlatformParametersConsts.TTL));
        if (optional.isPresent()) {
            return optional.map(Long::parseLong).get();
        }
        return null;
    }

    private void addSharedServiceResponse(Stack stack, StackV4Response stackResponse) {
        SharedServiceV4Response sharedServiceResponse = new SharedServiceV4Response();
        if (stack.getDatalakeResourceId() != null) {
            Optional<DatalakeResources> datalakeResources = datalakeResourcesService.getDatalakeResourcesById(stack.getDatalakeResourceId());
            if (datalakeResources.isPresent()) {
                DatalakeResources datalakeResource = datalakeResources.get();
                sharedServiceResponse.setSharedClusterId(datalakeResource.getDatalakeStackId());
                sharedServiceResponse.setSharedClusterName(datalakeResource.getName());
            }
        } else {
            DatalakeResources datalakeResources = datalakeResourcesService.getDatalakeResourcesByDatalakeStackId(stack.getId());
            if (datalakeResources != null) {
                for (Stack connectedStacks : stackService.findClustersConnectedToDatalakeByDatalakeResourceId(datalakeResources.getId())) {
                    AttachedClusterInfoV4Response attachedClusterInfoResponse = new AttachedClusterInfoV4Response();
                    attachedClusterInfoResponse.setId(connectedStacks.getId());
                    attachedClusterInfoResponse.setName(connectedStacks.getName());
                    sharedServiceResponse.getAttachedClusters().add(attachedClusterInfoResponse);
                }
            }
        }
        stackResponse.setSharedService(sharedServiceResponse);
    }

}
