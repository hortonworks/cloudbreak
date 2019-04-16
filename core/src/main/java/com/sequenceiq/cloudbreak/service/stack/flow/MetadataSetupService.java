package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.Clock;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;

@Service
public class MetadataSetupService {

    @Inject
    private ImageService imageService;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private Clock clock;

    public int saveInstanceMetaData(Stack stack, Iterable<CloudVmMetaDataStatus> cloudVmMetaDataStatusList, InstanceStatus status) {
        try {
            int newInstances = 0;
            Set<InstanceMetaData> allInstanceMetadata = instanceMetaDataService.findNotTerminatedForStack(stack.getId());
            boolean primaryIgSelected = allInstanceMetadata.stream().anyMatch(imd -> imd.getInstanceMetadataType() == InstanceMetadataType.GATEWAY_PRIMARY);
            Json imageJson = new Json(imageService.getImage(stack.getId()));
            for (CloudVmMetaDataStatus cloudVmMetaDataStatus : cloudVmMetaDataStatusList) {
                CloudInstance cloudInstance = cloudVmMetaDataStatus.getCloudVmInstanceStatus().getCloudInstance();
                CloudInstanceMetaData md = cloudVmMetaDataStatus.getMetaData();
                Long privateId = cloudInstance.getTemplate().getPrivateId();
                String instanceId = cloudInstance.getInstanceId();
                InstanceMetaData instanceMetaDataEntry = createInstanceMetadataIfAbsent(allInstanceMetadata, privateId, instanceId);
                if (instanceMetaDataEntry.getInstanceId() == null) {
                    newInstances++;
                }
                // CB 1.0.x clusters do not have private id thus we cannot correlate them with instance groups thus keep the original one
                InstanceGroup ig = instanceMetaDataEntry.getInstanceGroup();
                String group = ig == null ? cloudInstance.getTemplate().getGroupName() : ig.getGroupName();
                InstanceGroup instanceGroup = instanceGroupService.findOneByGroupNameInStack(stack.getId(), group).orElse(null);
                instanceMetaDataEntry.setPrivateIp(md.getPrivateIp());
                instanceMetaDataEntry.setPublicIp(md.getPublicIp());
                instanceMetaDataEntry.setSshPort(md.getSshPort());
                instanceMetaDataEntry.setLocalityIndicator(md.getLocalityIndicator());
                instanceMetaDataEntry.setInstanceGroup(instanceGroup);
                instanceMetaDataEntry.setInstanceId(instanceId);
                instanceMetaDataEntry.setPrivateId(privateId);
                instanceMetaDataEntry.setStartDate(clock.getCurrentTimeMillis());
                instanceMetaDataEntry.setSubnetId(cloudInstance.getStringParameter(CloudInstance.SUBNET_ID));
                instanceMetaDataEntry.setInstanceName(cloudInstance.getStringParameter(CloudInstance.INSTANCE_NAME));
                instanceMetaDataEntry.setAmbariServer(Boolean.FALSE);
                if (instanceMetaDataEntry.getInstanceMetadataType() == null) {
                    if (ig != null) {
                        if (InstanceGroupType.GATEWAY.equals(ig.getInstanceGroupType())) {
                            if (!primaryIgSelected) {
                                primaryIgSelected = true;
                                instanceMetaDataEntry.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
                                instanceMetaDataEntry.setAmbariServer(Boolean.TRUE);
                            } else {
                                instanceMetaDataEntry.setInstanceMetadataType(InstanceMetadataType.GATEWAY);
                            }
                        } else {
                            instanceMetaDataEntry.setInstanceMetadataType(InstanceMetadataType.CORE);
                        }
                    } else {
                        instanceMetaDataEntry.setInstanceMetadataType(InstanceMetadataType.CORE);
                    }
                }
                if (status != null) {
                    instanceMetaDataEntry.setInstanceStatus(status);
                    instanceMetaDataEntry.setImage(imageJson);
                }
                instanceMetaDataService.save(instanceMetaDataEntry);
            }
            return newInstances;
        } catch (CloudbreakImageNotFoundException | JsonProcessingException ex) {
            throw new CloudbreakServiceException("Instance metadata collection failed", ex);
        }
    }

    private InstanceMetaData createInstanceMetadataIfAbsent(Iterable<InstanceMetaData> allInstanceMetadata, Long privateId, String instanceId) {
        if (privateId != null) {
            for (InstanceMetaData instanceMetaData : allInstanceMetadata) {
                if (Objects.equals(instanceMetaData.getPrivateId(), privateId)) {
                    return instanceMetaData;
                }
            }
        } else {
            for (InstanceMetaData instanceMetaData : allInstanceMetadata) {
                if (Objects.equals(instanceMetaData.getInstanceId(), instanceId)) {
                    return instanceMetaData;
                }
            }
        }
        return new InstanceMetaData();
    }

}
