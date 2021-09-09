package com.sequenceiq.cloudbreak.service.decorator.responseprovider;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.StackResponseEntries;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.hardware.HardwareInfoGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.hardware.HardwareInfoV4Response;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.template.TemplateToInstanceTemplateV4ResponseConverter;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;

@Component
public class StackResponseHardwareInfoProvider implements ResponseProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackResponseHardwareInfoProvider.class);

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private TemplateToInstanceTemplateV4ResponseConverter templateToInstanceTemplateV4ResponseConverter;

    @Override
    public StackV4Response providerEntriesToStackResponse(Stack stack, StackV4Response stackResponse) {
        Set<HardwareInfoGroupV4Response> hardwareInfoResponses = stack.getInstanceGroups().stream()
                .map(instanceGroup -> hardwareInfoGroupResponse(stack, instanceGroup))
                .collect(Collectors.toSet());
        stackResponse.setHardwareInfoGroups(hardwareInfoResponses);
        return stackResponse;
    }

    @Override
    public String type() {
        return StackResponseEntries.HARDWARE_INFO.getEntryName();
    }

    private HardwareInfoGroupV4Response hardwareInfoGroupResponse(Stack stack, InstanceGroup instanceGroup) {

        Template template = instanceGroup.getTemplate();
        Set<InstanceMetaData> allInstanceMetaData = instanceGroup.getNotTerminatedInstanceMetaDataSet();
        Optional<HostGroup> hostGroup = Optional.empty();
        if (stack.getCluster() != null) {
            hostGroup = hostGroupService.findHostGroupInClusterByName(stack.getCluster().getId(), instanceGroup.getGroupName());
        }

        HardwareInfoGroupV4Response hardwareInfoGroupResponse = new HardwareInfoGroupV4Response();
        hostGroup.ifPresent(group -> hardwareInfoGroupResponse.setRecoveryMode(group.getRecoveryMode()));
        hardwareInfoGroupResponse.setName(instanceGroup.getGroupName());

        for (InstanceMetaData instanceMetaData : allInstanceMetaData) {

            HardwareInfoV4Response hardwareInfoResponse = new HardwareInfoV4Response();
            hardwareInfoResponse.setAmbariServer(instanceMetaData.getAmbariServer());
            hardwareInfoResponse.setDiscoveryFQDN(instanceMetaData.getDiscoveryFQDN());
            hardwareInfoResponse.setInstanceGroup(instanceMetaData.getInstanceGroupName());
            hardwareInfoResponse.setInstanceId(instanceMetaData.getInstanceId());
            hardwareInfoResponse.setInstanceStatus(instanceMetaData.getInstanceStatus());
            hardwareInfoResponse.setInstanceMetadataType(instanceMetaData.getInstanceMetadataType());
            hardwareInfoResponse.setPrivateIp(instanceMetaData.getPrivateIp());
            hardwareInfoResponse.setSubnetId(instanceMetaData.getSubnetId());
            hardwareInfoResponse.setAvailabilityZone(instanceMetaData.getAvailabilityZone());
            hardwareInfoResponse.setPublicIp(instanceMetaData.getPublicIp());
            hardwareInfoResponse.setSshPort(instanceMetaData.getSshPort());
            hardwareInfoResponse.setGroupName(instanceMetaData.getInstanceGroupName());
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

    private boolean isImagePresented(InstanceMetaData instanceMetaData) {
        return instanceMetaData.getImage() != null && instanceMetaData.getImage().getValue() != null;
    }
}
