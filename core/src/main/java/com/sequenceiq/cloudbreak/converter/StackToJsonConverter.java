package com.sequenceiq.cloudbreak.converter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.CloudbreakDetailsJson;
import com.sequenceiq.cloudbreak.api.model.ClusterResponse;
import com.sequenceiq.cloudbreak.api.model.CredentialResponse;
import com.sequenceiq.cloudbreak.api.model.FailurePolicyResponse;
import com.sequenceiq.cloudbreak.api.model.ImageJson;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupResponse;
import com.sequenceiq.cloudbreak.api.model.NetworkResponse;
import com.sequenceiq.cloudbreak.api.model.OrchestratorResponse;
import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.CloudbreakDetails;
import com.sequenceiq.cloudbreak.cloud.model.HDPRepo;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.StackTemplate;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.ComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.image.ImageService;

@Component
public class StackToJsonConverter extends AbstractConversionServiceAwareConverter<Stack, StackResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackToJsonConverter.class);

    @Inject
    private ImageService imageService;

    @Inject
    private ComponentConfigProvider componentConfigProvider;

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
        List<InstanceGroupResponse> templateGroups = new ArrayList<>();
        templateGroups.addAll(convertInstanceGroups(source.getInstanceGroups()));
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
        stackJson.setRelocateDocker(source.getRelocateDocker());
        stackJson.setParameters(source.getParameters());
        stackJson.setPlatformVariant(source.getPlatformVariant());
        if (source.getOrchestrator() != null) {
            stackJson.setOrchestrator(getConversionService().convert(source.getOrchestrator(), OrchestratorResponse.class));
        }
        stackJson.setCreated(source.getCreated());
        stackJson.setGatewayPort(source.getGatewayPort());
        List<Resource> resourcesByType = source.getResourcesByType(ResourceType.S3_ACCESS_ROLE_ARN);
        Optional<Resource> accessRoleArnOptional = resourcesByType.stream().findFirst();
        if (accessRoleArnOptional.isPresent()) {
            String s3AccessRoleArn = accessRoleArnOptional.get().getResourceName();
            stackJson.setS3AccessRoleArn(s3AccessRoleArn);
        }
        convertComponentConfig(stackJson, source.getId());
        convertTags(stackJson, source.getTags());
        return stackJson;
    }

    private void convertTags(StackResponse stackJson, Json tag) {
        try {
            Map<String, Object> tags = new HashMap<>();
            if (tag != null) {
                if (tag.getValue() != null) {
                    stackJson.setTags(tag.get(Map.class));
                } else {
                    stackJson.setTags(tags);
                }
            } else {
                stackJson.setTags(new HashMap<>());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to convert dynamic tags.", e);
        }
    }

    private StackResponse convertComponentConfig(StackResponse stackJson, Long stackId) {
        try {
            AmbariRepo ambariRepo = componentConfigProvider.getAmbariRepo(stackId);
            if (ambariRepo != null) {
                stackJson.setAmbariVersion(ambariRepo.getVersion());
            }
            HDPRepo hdpRepo = componentConfigProvider.getHDPRepo(stackId);
            if (hdpRepo != null) {
                stackJson.setHdpVersion(hdpRepo.getHdpVersion());
            }

            CloudbreakDetails cloudbreakDetails = componentConfigProvider.getCloudbreakDetails(stackId);
            if (cloudbreakDetails != null) {
                stackJson.setCloudbreakDetails(getConversionService().convert(cloudbreakDetails, CloudbreakDetailsJson.class));
            }

            StackTemplate stackTemplate = componentConfigProvider.getStackTemplate(stackId);
            if (stackTemplate != null) {
                stackJson.setStackTemplate(stackTemplate.getTemplate());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to convert dynamic component.", e);
        }

        return stackJson;
    }

    private Set<InstanceGroupResponse> convertInstanceGroups(Set<InstanceGroup> instanceGroups) {
        return (Set<InstanceGroupResponse>) getConversionService().convert(instanceGroups, TypeDescriptor.forObject(instanceGroups),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(InstanceGroupResponse.class)));
    }

}
