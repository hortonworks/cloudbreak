package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.model.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderMetadataAdapter;

@Service
public class MetadataSetupService {

    @Inject
    private ServiceProviderMetadataAdapter metadata;

    @Inject
    private InstanceGroupRepository instanceGroupRepository;

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    public int saveInstanceMetaData(Stack stack, List<CloudVmMetaDataStatus> cloudVmMetaDataStatusList, InstanceStatus status) {
        int newInstances = 0;
        boolean ambariServerFound = false;
        Set<InstanceMetaData> allInstanceMetadata = instanceMetaDataRepository.findNotTerminatedForStack(stack.getId());
        boolean primaryIgSelected = allInstanceMetadata.stream().anyMatch(imd -> imd.getInstanceMetadataType() == InstanceMetadataType.GATEWAY_PRIMARY);
        for (CloudVmMetaDataStatus cloudVmMetaDataStatus : cloudVmMetaDataStatusList) {
            CloudInstance cloudInstance = cloudVmMetaDataStatus.getCloudVmInstanceStatus().getCloudInstance();
            CloudInstanceMetaData md = cloudVmMetaDataStatus.getMetaData();
            long timeInMillis = Calendar.getInstance().getTimeInMillis();
            Long privateId = cloudInstance.getTemplate().getPrivateId();
            String instanceId = cloudInstance.getInstanceId();
            InstanceMetaData instanceMetaDataEntry = createInstanceMetadataIfAbsent(allInstanceMetadata, privateId, instanceId);
            if (instanceMetaDataEntry.getInstanceId() == null) {
                newInstances++;
            }
            // CB 1.0.x clusters do not have private id thus we cannot correlate them with instance groups thus keep the original one
            InstanceGroup ig = instanceMetaDataEntry.getInstanceGroup();
            String group = ig == null ? cloudInstance.getTemplate().getGroupName() : ig.getGroupName();
            InstanceGroup instanceGroup = instanceGroupRepository.findOneByGroupNameInStack(stack.getId(), group);
            instanceMetaDataEntry.setPrivateIp(md.getPrivateIp());
            instanceMetaDataEntry.setPublicIp(md.getPublicIp());
            instanceMetaDataEntry.setSshPort(md.getSshPort());
            instanceMetaDataEntry.setLocalityIndicator(md.getLocalityIndicator());
            instanceMetaDataEntry.setInstanceGroup(instanceGroup);
            instanceMetaDataEntry.setInstanceId(instanceId);
            instanceMetaDataEntry.setPrivateId(privateId);
            instanceMetaDataEntry.setStartDate(timeInMillis);
            instanceMetaDataEntry.setSubnetId(cloudInstance.getStringParameter(CloudInstance.SUBNET_ID));
            if (instanceMetaDataEntry.getInstanceMetadataType() == null) {
                if (ig != null) {
                    if (InstanceGroupType.GATEWAY.equals(ig.getInstanceGroupType())) {
                        if (!primaryIgSelected) {
                            primaryIgSelected = true;
                            instanceMetaDataEntry.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
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
            if (!ambariServerFound && InstanceGroupType.GATEWAY.equals(instanceGroup.getInstanceGroupType())) {
                instanceMetaDataEntry.setAmbariServer(Boolean.TRUE);
                ambariServerFound = true;
            } else {
                instanceMetaDataEntry.setAmbariServer(Boolean.FALSE);
            }
            if (status != null) {
                instanceMetaDataEntry.setInstanceStatus(status);
            }
            instanceMetaDataRepository.save(instanceMetaDataEntry);
        }
        return newInstances;
    }

    private InstanceMetaData createInstanceMetadataIfAbsent(Set<InstanceMetaData> allInstanceMetadata, Long privateId, String instanceId) {
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
