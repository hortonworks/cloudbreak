package com.sequenceiq.freeipa.service.stack.instance;

import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceLifeCycle;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;

@Service
public class MetadataSetupService {

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private Clock clock;

    public int saveInstanceMetaData(Stack stack, Iterable<CloudVmMetaDataStatus> cloudVmMetaDataStatusList, InstanceStatus status) {
        int newInstances = 0;
        Set<InstanceMetaData> allInstanceMetadata = instanceMetaDataService.findNotTerminatedForStack(stack.getId());
        boolean primaryIgSelected = allInstanceMetadata.stream().anyMatch(imd -> imd.getInstanceMetadataType() == InstanceMetadataType.GATEWAY_PRIMARY);
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
            instanceMetaDataEntry.setLifeCycle(InstanceLifeCycle.fromCloudInstanceLifeCycle(md.getLifeCycle()));
            if (instanceMetaDataEntry.getInstanceMetadataType() == null) {
                if (ig != null) {
                    if (!primaryIgSelected && ig.getInstanceGroupType()
                            == com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType.MASTER) {
                        primaryIgSelected = true;
                        instanceMetaDataEntry.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
                    } else {
                        instanceMetaDataEntry.setInstanceMetadataType(InstanceMetadataType.GATEWAY);
                    }
                } else {
                    instanceMetaDataEntry.setInstanceMetadataType(InstanceMetadataType.GATEWAY);
                }
            }
            if (status != null) {
                instanceMetaDataEntry.setInstanceStatus(status);
            }
            instanceMetaDataService.save(instanceMetaDataEntry);
        }
        return newInstances;
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
