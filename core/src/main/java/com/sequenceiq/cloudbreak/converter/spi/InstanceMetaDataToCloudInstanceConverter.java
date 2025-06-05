package com.sequenceiq.cloudbreak.converter.spi;

import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_ID;
import static com.sequenceiq.cloudbreak.util.NullUtil.putIfPresent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Joiner;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.converter.InstanceMetadataToImageIdConverter;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class InstanceMetaDataToCloudInstanceConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceMetaDataToCloudInstanceConverter.class);

    @Inject
    private StackToCloudStackConverter stackToCloudStackConverter;

    @Inject
    private InstanceMetadataToImageIdConverter instanceMetadataToImageIdConverter;

    @Inject
    private EnvironmentService environmentClientService;

    @Inject
    private InstanceGroupService instanceGroupService;

    public List<CloudInstance> convert(List<InstanceGroupDto> instanceGroups, StackView stackView) {
        List<CloudInstance> cloudInstances = new ArrayList<>();
        for (InstanceGroupDto instanceGroup : instanceGroups) {
            for (InstanceMetadataView metaDataEntity : instanceGroup.getNotDeletedAndNotZombieInstanceMetaData()) {
                cloudInstances.add(convert(metaDataEntity, instanceGroup.getInstanceGroup(), stackView));
            }
        }
        return cloudInstances;
    }

    public List<CloudInstance> convert(Collection<InstanceMetadataView> instanceMetadataViews, StackView stack) {
        List<CloudInstance> cloudInstances = new ArrayList<>();
        Set<String> instanceGroupNames = instanceMetadataViews.stream().map(im -> im.getInstanceGroupName()).collect(Collectors.toSet());
        List<InstanceGroupView> instanceGroupViews = instanceGroupService.findAllInstanceGroupViewByStackIdAndGroupName(stack.getId(), instanceGroupNames);
        if (instanceGroupViews.size() != instanceGroupNames.size()) {
            Set<String> fetchedInstanceGroupNames = instanceGroupViews.stream()
                    .map(ig -> String.format("[name: %s, type: %s]", ig.getGroupName(), ig.getInstanceGroupType()))
                    .collect(Collectors.toSet());
            LOGGER.warn("Cannot find all of the instance groups ({}) for names: {}",
                    Joiner.on(",").join(fetchedInstanceGroupNames),
                    Joiner.on(",").join(instanceGroupNames));
        }
        DetailedEnvironmentResponse environment = environmentClientService.getByCrnAsInternal(stack.getEnvironmentCrn());
        for (InstanceMetadataView instanceMetadataView : instanceMetadataViews) {
            InstanceGroupView instanceGroupView = instanceGroupViews.stream()
                    .filter(ig -> ig.getGroupName().equals(instanceMetadataView.getInstanceGroupName()))
                    .findFirst()
                    .orElseThrow(NotFoundException.notFound("InstanceGroup", instanceMetadataView.getInstanceGroupName()));
            cloudInstances.add(convert(instanceMetadataView, instanceGroupView, stack, environment));
        }
        return cloudInstances;
    }

    public CloudInstance convert(InstanceMetadataView metaDataEntity, InstanceGroupView group, StackView stackView) {
        DetailedEnvironmentResponse environment = environmentClientService.getByCrnAsInternal(stackView.getEnvironmentCrn());
        return convert(metaDataEntity, group, stackView, environment);
    }

    private CloudInstance convert(InstanceMetadataView metaDataEntity, InstanceGroupView group, StackView stackView,
            DetailedEnvironmentResponse environment) {
        Template template = group.getTemplate();
        InstanceStatus status = getInstanceStatus(metaDataEntity);
        String imageId = instanceMetadataToImageIdConverter.convert(metaDataEntity);
        InstanceTemplate instanceTemplate = stackToCloudStackConverter.buildInstanceTemplate(template, group.getGroupName(), metaDataEntity.getPrivateId(),
                status, imageId);
        StackAuthentication stackAuthentication = stackView.getStackAuthentication();
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication(
                stackAuthentication.getPublicKey(),
                stackAuthentication.getPublicKeyId(),
                stackAuthentication.getLoginUserName());
        Map<String, Object> params = new HashMap<>();
        putIfPresent(params, SUBNET_ID, metaDataEntity.getSubnetId());
        putIfPresent(params, CloudInstance.INSTANCE_NAME, metaDataEntity.getInstanceName());
        Map<String, Object> cloudInstanceParameters = stackToCloudStackConverter.buildCloudInstanceParameters(
                environment,
                stackView,
                metaDataEntity,
                CloudPlatform.valueOf(template.getCloudPlatform()),
                group.getTemplate());
        params.putAll(cloudInstanceParameters);

        return new CloudInstance(
                metaDataEntity.getInstanceId(),
                instanceTemplate,
                instanceAuthentication,
                metaDataEntity.getSubnetId(),
                metaDataEntity.getAvailabilityZone(),
                params);
    }

    private InstanceStatus getInstanceStatus(InstanceMetadataView metaData) {
        switch (metaData.getInstanceStatus()) {
            case REQUESTED:
                return InstanceStatus.CREATE_REQUESTED;
            case CREATED:
                return InstanceStatus.CREATED;
            case SERVICES_RUNNING:
            case SERVICES_HEALTHY:
            case SERVICES_UNHEALTHY:
                return InstanceStatus.STARTED;
            // TODO ZZZ: Using DECOMMISSIONED is a bad idea based on this conversion. DECOMMISSIONED somehow means DELETE_REQUESTED, which
            //  is going to cause chaos on the user side. SERVICES_RUNNING is just an extremely STUPID state.
            case DECOMMISSIONED:
            case DELETE_REQUESTED:
                return InstanceStatus.DELETE_REQUESTED;
            case TERMINATED:
                return InstanceStatus.TERMINATED;
            case DELETED_BY_PROVIDER:
                return InstanceStatus.TERMINATED_BY_PROVIDER;
            default:
                return InstanceStatus.UNKNOWN;
        }
    }
}
