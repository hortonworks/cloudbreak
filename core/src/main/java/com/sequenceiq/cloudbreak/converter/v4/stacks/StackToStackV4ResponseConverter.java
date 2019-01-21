package com.sequenceiq.cloudbreak.converter.v4.stacks;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.api.endpoint.v4.flexsubscription.responses.FlexSubscriptionV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.CloudbreakDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.customdomain.CustomDomainSettingsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.environment.EnvironmentSettingsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.StackImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.network.NetworkV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.stackauthentication.StackAuthenticationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.tags.TagsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudbreakDetails;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.ComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.image.ImageService;

@Component
public class StackToStackV4ResponseConverter extends AbstractConversionServiceAwareConverter<Stack, StackV4Response> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackToStackV4ResponseConverter.class);

    @Inject
    private ImageService imageService;

    @Inject
    private ComponentConfigProvider componentConfigProvider;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private ProviderParameterCalculator providerParameterCalculator;

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
        }
        response.setStatus(source.getStatus());
        response.setTerminated(source.getTerminated());
        response.setStatusReason(source.getStatusReason());
        response.setInstanceGroups(converterUtil.convertAll(source.getInstanceGroups(), InstanceGroupV4Response.class));
        response.setCluster(getConversionService().convert(source.getCluster(), ClusterV4Response.class));
        response.setNetwork(getConversionService().convert(source.getNetwork(), NetworkV4Response.class));
        providerParameterCalculator.to(new HashMap<>(source.getParameters()), response);
        response.setCreated(source.getCreated());
        response.setGatewayPort(source.getGatewayPort());
        response.setCustomDomains(getConversionService().convert(source, CustomDomainSettingsV4Response.class));
        response.setWorkspace(getConversionService().convert(source.getWorkspace(), WorkspaceResourceV4Response.class));
        addNodeCount(source, response);
        putS3RoleIntoResponse(source, response);
        convertComponentConfig(response, source);
        response.setTags(getTags(response, source.getTags()));
        addFlexSubscription(response, source);
        return response;
    }

    private void addNodeCount(Stack source, StackV4Response stackJson) {
        int nodeCount = 0;
        for (InstanceGroup instanceGroup : source.getInstanceGroups()) {
            nodeCount += instanceGroup.getNodeCount();
        }
        stackJson.setNodeCount(nodeCount);
    }

    private void putS3RoleIntoResponse(Stack source, StackV4Response stackResponse) {
        List<Resource> resourcesByType = source.getResourcesByType(ResourceType.S3_ACCESS_ROLE_ARN);
        Optional<Resource> accessRoleArnOptional = resourcesByType.stream().findFirst();
        if (accessRoleArnOptional.isPresent()) {
            String s3AccessRoleArn = accessRoleArnOptional.get().getResourceName();
            stackResponse.getAws().setAwsS3Role(s3AccessRoleArn);
        }
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
                        Set<String> recipeNames = hostGroup.getRecipes().stream().map(Recipe::getName).collect(Collectors.toSet());
                        instanceGroupResponse.setRecipes(converterUtil.convertAll(hostGroup.getRecipes(), RecipeV4Response.class));
                        instanceGroupResponse.setRecoveryMode(hostGroup.getRecoveryMode());
                    });
        }
    }
}
