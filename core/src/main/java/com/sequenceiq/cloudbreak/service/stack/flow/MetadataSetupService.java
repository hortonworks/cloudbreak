package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderMetadataAdapter;

@Service
public class MetadataSetupService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataSetupService.class);
    @Inject
    private ServiceProviderMetadataAdapter metadata;
    @Inject
    private InstanceGroupRepository instanceGroupRepository;
    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;
    @Inject
    private ClusterService clusterService;

    public Set<InstanceMetaData> saveInstanceMetaData(Stack stack, List<CloudVmMetaDataStatus> cloudVmMetaDataStatusList, InstanceStatus status) {
        Boolean ambariServerFound = false;
        Set<InstanceMetaData> updatedInstanceMetadata = new HashSet<>();
        Set<InstanceMetaData> allInstanceMetadata = instanceMetaDataRepository.findNotTerminatedForStack(stack.getId());
        for (CloudVmMetaDataStatus cloudVmMetaDataStatus : cloudVmMetaDataStatusList) {
            CloudInstance cloudInstance = cloudVmMetaDataStatus.getCloudVmInstanceStatus().getCloudInstance();
            CloudInstanceMetaData md = cloudVmMetaDataStatus.getMetaData();
            long timeInMillis = Calendar.getInstance().getTimeInMillis();
            Long privateId = cloudInstance.getTemplate().getPrivateId();
            String instanceId = cloudInstance.getInstanceId();
            InstanceMetaData instanceMetaDataEntry = createInstanceMetadataIfAbsent(allInstanceMetadata, privateId, instanceId);
            // CB 1.0.x clusters do not have private id thus we cannot correlate them with instance groups thus keep the original one
            String group = instanceMetaDataEntry.getInstanceGroup() == null ? cloudInstance.getTemplate().getGroupName() : instanceMetaDataEntry
                    .getInstanceGroup().getGroupName();
            InstanceGroup instanceGroup = instanceGroupRepository.findOneByGroupNameInStack(stack.getId(), group);
            instanceMetaDataEntry.setPrivateIp(md.getPrivateIp());
            instanceMetaDataEntry.setPublicIp(md.getPublicIp());
            instanceMetaDataEntry.setSshPort(md.getSshPort());
            instanceMetaDataEntry.setHypervisor(md.getHypervisor());
            instanceMetaDataEntry.setInstanceGroup(instanceGroup);
            instanceMetaDataEntry.setInstanceId(instanceId);
            instanceMetaDataEntry.setPrivateId(privateId);
            instanceMetaDataEntry.setStartDate(timeInMillis);
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
            updatedInstanceMetadata.add(instanceMetaDataEntry);
        }
        return updatedInstanceMetadata;
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
