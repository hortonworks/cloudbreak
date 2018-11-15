package com.sequenceiq.cloudbreak.converter.stack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.model.CloudbreakDetailsJson;
import com.sequenceiq.cloudbreak.api.model.CredentialResponse;
import com.sequenceiq.cloudbreak.api.model.FailurePolicyResponse;
import com.sequenceiq.cloudbreak.api.model.FlexSubscriptionResponse;
import com.sequenceiq.cloudbreak.api.model.ImageJson;
import com.sequenceiq.cloudbreak.api.model.NetworkResponse;
import com.sequenceiq.cloudbreak.api.model.OrchestratorResponse;
import com.sequenceiq.cloudbreak.api.model.stack.StackAuthenticationResponse;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterResponse;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupResponse;
import com.sequenceiq.cloudbreak.api.model.users.WorkspaceResourceResponse;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.CloudbreakDetails;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.ComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.image.ImageService;

@Component
public class StackToStackResponseConverter extends AbstractConversionServiceAwareConverter<Stack, StackResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackToStackResponseConverter.class);

    private static final String SUBNET_ID = "subnetId";

    private static final String NETWORK_ID = "networkId";

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Inject
    private ImageService imageService;

    @Inject
    private ComponentConfigProvider componentConfigProvider;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Override
    public StackResponse convert(Stack source) {
        StackResponse stackJson = new StackResponse();
        try {
            Image image = imageService.getImage(source.getId());
            stackJson.setImage(getConversionService().convert(image, ImageJson.class));
        } catch (CloudbreakImageNotFoundException exc) {
            LOGGER.info(exc.getMessage());
        }

        stackJson.setName(source.getName());
        stackJson.setStackAuthentication(conversionService.convert(source.getStackAuthentication(), StackAuthenticationResponse.class));
        stackJson.setId(source.getId());
        if (source.getCredential() == null) {
            stackJson.setCloudPlatform(null);
            stackJson.setCredentialId(null);
        } else {
            stackJson.setCloudPlatform(source.cloudPlatform());
            stackJson.setCredentialId(source.getCredential().getId());
            stackJson.setCredential(getConversionService().convert(source.getCredential(), CredentialResponse.class));
        }
        stackJson.setStatus(source.getStatus());
        stackJson.setStatusReason(source.getStatusReason());
        stackJson.setRegion(source.getRegion());
        stackJson.setAvailabilityZone(source.getAvailabilityZone());
        stackJson.setOnFailureAction(source.getOnFailureActionAction());
        List<InstanceGroupResponse> templateGroups = new ArrayList<>(convertInstanceGroups(source.getInstanceGroups()));
        stackJson.setInstanceGroups(templateGroups);
        if (source.getCluster() != null) {
            stackJson.setCluster(getConversionService().convert(source.getCluster(), ClusterResponse.class));
        } else {
            stackJson.setCluster(new ClusterResponse());
        }
        if (source.getFailurePolicy() != null) {
            stackJson.setFailurePolicy(getConversionService().convert(source.getFailurePolicy(), FailurePolicyResponse.class));
        }
        if (source.getNetwork() == null) {
            stackJson.setNetworkId(null);
        } else {
            stackJson.setNetworkId(source.getNetwork().getId());
            stackJson.setNetwork(getConversionService().convert(source.getNetwork(), NetworkResponse.class));
        }
        stackJson.setParameters(Maps.newHashMap(source.getParameters()));
        stackJson.setPlatformVariant(source.getPlatformVariant());
        if (source.getOrchestrator() != null) {
            stackJson.setOrchestrator(getConversionService().convert(source.getOrchestrator(), OrchestratorResponse.class));
        }
        stackJson.setCreated(source.getCreated());
        stackJson.setGatewayPort(source.getGatewayPort());
        stackJson.setCustomDomain(source.getCustomDomain());
        stackJson.setCustomHostname(source.getCustomHostname());
        stackJson.setClusterNameAsSubdomain(source.isClusterNameAsSubdomain());
        stackJson.setHostgroupNameAsHostname(source.isHostgroupNameAsHostname());
        stackJson.setWorkspace(conversionService.convert(source.getWorkspace(), WorkspaceResourceResponse.class));
        addNodeCount(source, stackJson);
        putSubnetIdIntoResponse(source, stackJson);
        putVpcIdIntoResponse(source, stackJson);
        putS3RoleIntoResponse(source, stackJson);
        convertComponentConfig(stackJson, source);
        convertTags(stackJson, source.getTags());
        addFlexSubscription(stackJson, source);
        if (source.getEnvironment() != null) {
            stackJson.setEnvironment(source.getEnvironment().getName());
        }
        return stackJson;
    }

    private void addNodeCount(Stack source, StackResponse stackJson) {
        int nodeCount = 0;
        for (InstanceGroup instanceGroup : source.getInstanceGroups()) {
            nodeCount += instanceGroup.getNodeCount();
        }
        stackJson.setNodeCount(nodeCount);
    }

    private void putSubnetIdIntoResponse(Stack source, StackResponse stackResponse) {
        if (stackResponse.getNetwork() != null) {
            findAndAddResource(source, stackResponse.getNetwork().getParameters(), SUBNET_ID, ResourceType.AWS_SUBNET);
            findAndAddResource(source, stackResponse.getNetwork().getParameters(), SUBNET_ID, ResourceType.OPENSTACK_SUBNET);
            findAndAddResource(source, stackResponse.getNetwork().getParameters(), SUBNET_ID, ResourceType.GCP_SUBNET);
            findAndAddResource(source, stackResponse.getNetwork().getParameters(), SUBNET_ID, ResourceType.AZURE_SUBNET);
        }

        findAndAddResource(source, stackResponse.getParameters(), ResourceType.AWS_SUBNET.name(), ResourceType.AWS_SUBNET);
    }

    private void putVpcIdIntoResponse(Stack source, StackResponse stackResponse) {
        if (stackResponse.getNetwork() != null) {
            findAndAddResource(source, stackResponse.getNetwork().getParameters(), NETWORK_ID, ResourceType.AWS_VPC);
            findAndAddResource(source, stackResponse.getNetwork().getParameters(), NETWORK_ID, ResourceType.OPENSTACK_NETWORK);
            findAndAddResource(source, stackResponse.getNetwork().getParameters(), NETWORK_ID, ResourceType.GCP_NETWORK);
            findAndAddResource(source, stackResponse.getNetwork().getParameters(), NETWORK_ID, ResourceType.AZURE_NETWORK);
        }

        findAndAddResource(source, stackResponse.getParameters(), ResourceType.AWS_VPC.name(), ResourceType.AWS_VPC);
    }

    private void findAndAddResource(Stack source, Map<String, ? super String> parameters, String key, ResourceType resourceType) {
        List<Resource> resourcesByType = source.getResourcesByType(resourceType);
        Optional<Resource> resource = resourcesByType.stream().findFirst();

        if (resource.isPresent()) {
            String resourceName = resource.get().getResourceName();
            parameters.put(key, resourceName);
        }
    }

    private void putS3RoleIntoResponse(Stack source, StackResponse stackResponse) {
        List<Resource> resourcesByType = source.getResourcesByType(ResourceType.S3_ACCESS_ROLE_ARN);
        Optional<Resource> accessRoleArnOptional = resourcesByType.stream().findFirst();
        if (accessRoleArnOptional.isPresent()) {
            String s3AccessRoleArn = accessRoleArnOptional.get().getResourceName();
            stackResponse.getParameters().put(ResourceType.AWS_S3_ROLE.name(), s3AccessRoleArn);
        }
    }

    private void convertTags(StackResponse stackJson, Json tag) {
        try {
            if (tag != null) {
                if (tag.getValue() != null) {
                    StackTags stackTag = tag.get(StackTags.class);
                    stackJson.setApplicationTags(stackTag.getApplicationTags());
                    stackJson.setDefaultTags(stackTag.getDefaultTags());
                    stackJson.setUserDefinedTags(stackTag.getUserDefinedTags());
                } else {
                    stackJson.setApplicationTags(new HashMap<>());
                    stackJson.setDefaultTags(new HashMap<>());
                    stackJson.setUserDefinedTags(new HashMap<>());
                }
            } else {
                stackJson.setApplicationTags(new HashMap<>());
                stackJson.setDefaultTags(new HashMap<>());
                stackJson.setUserDefinedTags(new HashMap<>());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to convert dynamic tags.", e);
        }
    }

    private void convertComponentConfig(StackResponse stackJson, Stack source) {
        try {
            if (source.getCluster() != null) {
                StackRepoDetails stackRepoDetails = clusterComponentConfigProvider.getHDPRepo(source.getCluster().getId());
                if (stackRepoDetails != null && stackRepoDetails.getStack() != null) {

                    String repositoryVersion = stackRepoDetails.getStack().get(StackRepoDetails.REPOSITORY_VERSION);
                    if (!StringUtils.isEmpty(repositoryVersion)) {
                        stackJson.setHdpVersion(repositoryVersion);
                    } else {
                        stackJson.setHdpVersion(stackRepoDetails.getHdpVersion());
                    }
                }

                AmbariRepo ambariRepo = clusterComponentConfigProvider.getAmbariRepo(source.getCluster().getId());
                if (ambariRepo != null) {
                    stackJson.setAmbariVersion(ambariRepo.getVersion());
                }
            }
            CloudbreakDetails cloudbreakDetails = componentConfigProvider.getCloudbreakDetails(source.getId());
            if (cloudbreakDetails != null) {
                stackJson.setCloudbreakDetails(getConversionService().convert(cloudbreakDetails, CloudbreakDetailsJson.class));
            }
        } catch (RuntimeException e) {
            LOGGER.error("Failed to convert dynamic component.", e);
        }

    }

    private Set<InstanceGroupResponse> convertInstanceGroups(Set<InstanceGroup> instanceGroups) {
        return (Set<InstanceGroupResponse>) getConversionService().convert(instanceGroups, TypeDescriptor.forObject(instanceGroups),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(InstanceGroupResponse.class)));
    }

    private void addFlexSubscription(StackResponse stackJson, Stack source) {
        if (source.getFlexSubscription() != null) {
            try {
                FlexSubscriptionResponse flexSubscription = getConversionService().convert(source.getFlexSubscription(), FlexSubscriptionResponse.class);
                stackJson.setFlexSubscription(flexSubscription);
            } catch (Exception ex) {
                LOGGER.warn("Flex subscription could not be added to stack response.", ex);
            }
        }
    }
}
