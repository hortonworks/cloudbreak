package com.sequenceiq.cloudbreak.converter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.CloudbreakDetailsJson;
import com.sequenceiq.cloudbreak.api.model.ClusterResponse;
import com.sequenceiq.cloudbreak.api.model.CredentialResponse;
import com.sequenceiq.cloudbreak.api.model.FailurePolicyResponse;
import com.sequenceiq.cloudbreak.api.model.FlexSubscriptionResponse;
import com.sequenceiq.cloudbreak.api.model.ImageJson;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupResponse;
import com.sequenceiq.cloudbreak.api.model.NetworkResponse;
import com.sequenceiq.cloudbreak.api.model.OrchestratorResponse;
import com.sequenceiq.cloudbreak.api.model.StackAuthenticationResponse;
import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.CloudbreakDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.ComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.image.ImageService;

@Component
public class StackToJsonConverter extends AbstractConversionServiceAwareConverter<Stack, StackResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackToJsonConverter.class);

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Inject
    private ImageService imageService;

    @Inject
    private ComponentConfigProvider componentConfigProvider;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private HostMetadataRepository hostMetadataRepository;

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
        stackJson.setOwner(source.getOwner());
        stackJson.setAccount(source.getAccount());
        stackJson.setPublicInAccount(source.isPublicInAccount());
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
        stackJson.setParameters(new HashMap(source.getParameters()));
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
        addNodeCount(source, stackJson);
        putSubnetIdIntoResponse(source, stackJson);
        putVpcIdIntoResponse(source, stackJson);
        putS3RoleIntoResponse(source, stackJson);
        convertComponentConfig(stackJson, source);
        convertTags(stackJson, source.getTags());
        addFlexSubscription(stackJson, source);
        return stackJson;
    }

    private void addNodeCount(Stack source, StackResponse stackJson) {
        int nodeCount = 0;
        for (InstanceGroup instanceGroup : source.getInstanceGroups()) {
            nodeCount += instanceGroup.getNodeCount();
        }
        stackJson.setNodeCount(nodeCount);
    }

    private void putSubnetIdIntoResponse(Stack source, StackResponse stackJson) {
        List<Resource> resourcesByType = source.getResourcesByType(ResourceType.AWS_SUBNET);
        Optional<Resource> awsSubnet = resourcesByType.stream().findFirst();
        if (awsSubnet.isPresent()) {
            String subnetId = awsSubnet.get().getResourceName();
            stackJson.getParameters().put(ResourceType.AWS_SUBNET.name(), subnetId);
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

    private void putVpcIdIntoResponse(Stack source, StackResponse stackJson) {
        List<Resource> resourcesByType = source.getResourcesByType(ResourceType.AWS_VPC);
        Optional<Resource> awsVpc = resourcesByType.stream().findFirst();
        if (awsVpc.isPresent()) {
            String vpcId = awsVpc.get().getResourceName();
            stackJson.getParameters().put(ResourceType.AWS_VPC.name(), vpcId);
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

    private StackResponse convertComponentConfig(StackResponse stackJson, Stack source) {
        try {
            if (source.getCluster() != null) {
                StackRepoDetails stackRepoDetails = clusterComponentConfigProvider.getHDPRepo(source.getCluster().getId());
                if (stackRepoDetails != null) {
                    stackJson.setHdpVersion(stackRepoDetails.getHdpVersion());
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

        return stackJson;
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
