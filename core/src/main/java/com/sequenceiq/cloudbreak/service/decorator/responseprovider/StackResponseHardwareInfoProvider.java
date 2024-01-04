package com.sequenceiq.cloudbreak.service.decorator.responseprovider;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RecoveryMode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.StackResponseEntries;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.hardware.HardwareInfoGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.hardware.HardwareInfoV4Response;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.template.TemplateToInstanceTemplateV4ResponseConverter;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@Component
public class StackResponseHardwareInfoProvider implements ResponseProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackResponseHardwareInfoProvider.class);

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private TemplateToInstanceTemplateV4ResponseConverter templateToInstanceTemplateV4ResponseConverter;

    @Override
    public StackV4Response providerEntriesToStackResponse(StackDtoDelegate stack, StackV4Response stackResponse) {
        Set<HardwareInfoGroupV4Response> hardwareInfoResponses = stack.getInstanceGroupDtos().stream()
                .map(instanceGroup -> hardwareInfoGroupResponse(stack, instanceGroup.getInstanceGroup(), instanceGroup.getInstanceMetadataViews()))
                .collect(Collectors.toSet());
        stackResponse.setHardwareInfoGroups(hardwareInfoResponses);
        return stackResponse;
    }

    @Override
    public String type() {
        return StackResponseEntries.HARDWARE_INFO.getEntryName();
    }

    private HardwareInfoGroupV4Response hardwareInfoGroupResponse(StackDtoDelegate stack, InstanceGroupView instanceGroup,
            List<InstanceMetadataView> instanceMetadataViews) {

        Template template = instanceGroup.getTemplate();
        Optional<RecoveryMode> recoveryMode = Optional.empty();
        if (stack.getCluster() != null) {
            recoveryMode = hostGroupService.getRecoveryModeForHostGroup(stack.getCluster().getId(), instanceGroup.getGroupName());
        }

        HardwareInfoGroupV4Response hardwareInfoGroupResponse = new HardwareInfoGroupV4Response();
        recoveryMode.ifPresent(hardwareInfoGroupResponse::setRecoveryMode);
        hardwareInfoGroupResponse.setName(instanceGroup.getGroupName());

        for (InstanceMetadataView instanceMetaData : instanceMetadataViews) {

            HardwareInfoV4Response hardwareInfoResponse = new HardwareInfoV4Response();
            hardwareInfoResponse.setAmbariServer(instanceMetaData.getAmbariServer());
            hardwareInfoResponse.setDiscoveryFQDN(instanceMetaData.getDiscoveryFQDN());
            hardwareInfoResponse.setInstanceGroup(instanceGroup.getGroupName());
            hardwareInfoResponse.setInstanceId(instanceMetaData.getInstanceId());
            hardwareInfoResponse.setInstanceStatus(instanceMetaData.getInstanceStatus());
            hardwareInfoResponse.setInstanceMetadataType(instanceMetaData.getInstanceMetadataType());
            hardwareInfoResponse.setPrivateIp(instanceMetaData.getPrivateIp());
            hardwareInfoResponse.setSubnetId(instanceMetaData.getSubnetId());
            hardwareInfoResponse.setAvailabilityZone(instanceMetaData.getAvailabilityZone());
            hardwareInfoResponse.setPublicIp(instanceMetaData.getPublicIp());
            hardwareInfoResponse.setSshPort(instanceMetaData.getSshPort());
            hardwareInfoResponse.setGroupName(instanceGroup.getGroupName());
            hardwareInfoResponse.setName(instanceMetaData.getInstanceName());
            hardwareInfoResponse.setState(instanceMetaData.getInstanceStatus().getAsHostState());

            if (stack.getCluster() != null) {
                if (template != null) {
                    hardwareInfoResponse.setTemplate(templateToInstanceTemplateV4ResponseConverter.convert(template));
                }
            }

            try {
                if (isImagePresented(instanceMetaData)) {
                    Image image = instanceMetaData.getImage().get(Image.class);
                    if (image != null) {
                        hardwareInfoResponse.setImageCatalogName(image.getImageCatalogName());
                        hardwareInfoResponse.setImageName(image.getImageName());
                        hardwareInfoResponse.setImageId(image.getImageId());
                        hardwareInfoResponse.setImageCatalogUrl(image.getImageCatalogUrl());
                        hardwareInfoResponse.setOs(image.getOs());
                        hardwareInfoResponse.setOsType(image.getOsType());
                        hardwareInfoResponse.setPackageVersions(image.getPackageVersions());
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Could not deserialize image json on instancemetadata id: {} because attributes was: '{}'",
                        instanceMetaData.getId(), instanceMetaData.getImage());
            }

            hardwareInfoGroupResponse.getHardwareInfos().add(hardwareInfoResponse);
        }
        return hardwareInfoGroupResponse;
    }

    private boolean isImagePresented(InstanceMetadataView instanceMetaData) {
        return instanceMetaData.getImage() != null && instanceMetaData.getImage().getValue() != null;
    }
}
