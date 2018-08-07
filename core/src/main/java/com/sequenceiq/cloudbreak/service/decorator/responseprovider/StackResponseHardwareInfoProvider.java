package com.sequenceiq.cloudbreak.service.decorator.responseprovider;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.TemplateResponse;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponseEntries;
import com.sequenceiq.cloudbreak.api.model.stack.hardware.HardwareInfoGroupResponse;
import com.sequenceiq.cloudbreak.api.model.stack.hardware.HardwareInfoResponse;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;

@Component
public class StackResponseHardwareInfoProvider implements ResponseProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackResponseHardwareInfoProvider.class);

    @Inject
    private HostMetadataRepository hostMetadataRepository;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Override
    public StackResponse providerEntriesToStackResponse(Stack stack, StackResponse stackResponse) {
        Set<HardwareInfoGroupResponse> hardwareInfoResponses = stack.getInstanceGroups().stream()
                .map(instanceGroup -> hardwareInfoGroupResponse(stack, instanceGroup))
                .collect(Collectors.toSet());
        stackResponse.setHardwareInfoGroups(hardwareInfoResponses);
        return stackResponse;
    }

    @Override
    public String type() {
        return StackResponseEntries.HARDWARE_INFO.getEntryName();
    }

    private HardwareInfoGroupResponse hardwareInfoGroupResponse(Stack stack, InstanceGroup instanceGroup) {

        Template template = instanceGroup.getTemplate();
        Set<InstanceMetaData> allInstanceMetaData = instanceGroup.getAllInstanceMetaData();
        HostGroup hostGroup = null;
        if (stack.getCluster() != null) {
            hostGroup = hostGroupService.getByClusterIdAndName(stack.getCluster().getId(), instanceGroup.getGroupName());
        }

        HardwareInfoGroupResponse hardwareInfoGroupResponse = new HardwareInfoGroupResponse();
        if (hostGroup != null) {
            hardwareInfoGroupResponse.setRecoveryMode(hostGroup.getRecoveryMode());
        }
        hardwareInfoGroupResponse.setName(instanceGroup.getGroupName());


        for (InstanceMetaData instanceMetaData : allInstanceMetaData) {

            HardwareInfoResponse hardwareInfoResponse = new HardwareInfoResponse();
            hardwareInfoResponse.setAmbariServer(instanceMetaData.getAmbariServer());
            hardwareInfoResponse.setDiscoveryFQDN(instanceMetaData.getDiscoveryFQDN());
            hardwareInfoResponse.setInstanceGroup(instanceMetaData.getInstanceGroupName());
            hardwareInfoResponse.setInstanceId(instanceMetaData.getInstanceId());
            hardwareInfoResponse.setInstanceStatus(instanceMetaData.getInstanceStatus());
            hardwareInfoResponse.setInstanceMetadataType(instanceMetaData.getInstanceMetadataType());
            hardwareInfoResponse.setPrivateIp(instanceMetaData.getPrivateIp());
            hardwareInfoResponse.setPublicIp(instanceMetaData.getPublicIp());
            hardwareInfoResponse.setSshPort(instanceMetaData.getSshPort());

            HostMetadata hostMetadata = null;
            if (stack.getCluster() != null) {
                hostMetadata = hostMetadataRepository.findHostInClusterByName(stack.getCluster().getId(), instanceMetaData.getDiscoveryFQDN());
                if (template != null) {
                    hardwareInfoResponse.setTemplate(conversionService.convert(template, TemplateResponse.class));
                }
            }

            if (hostMetadata != null) {
                hardwareInfoResponse.setGroupName(hostMetadata.getHostGroup().getName());
                hardwareInfoResponse.setName(hostMetadata.getHostName());
                hardwareInfoResponse.setState(hostMetadata.getHostMetadataState().name());
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
                LOGGER.warn("Could not deserialize image json on instancemetadata id: {} because attributes was: '{}'",
                        instanceMetaData.getId(), instanceMetaData.getImage());
            }

            hardwareInfoGroupResponse.getHardwareInfos().add(hardwareInfoResponse);
        }
        return hardwareInfoGroupResponse;
    }

    private boolean isImagePresented(InstanceMetaData instanceMetaData) {
        return instanceMetaData.getImage() != null && instanceMetaData.getImage().getValue() != null;
    }
}
