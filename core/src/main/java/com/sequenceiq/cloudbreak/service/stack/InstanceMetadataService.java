package com.sequenceiq.cloudbreak.service.stack;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;

@Service
public class InstanceMetadataService {

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    public void updateInstanceStatus(Set<InstanceGroup> instanceGroup,
            Map<InstanceGroupType, com.sequenceiq.cloudbreak.api.model.InstanceStatus> newStatusByGroupType) {
        for (InstanceGroup group : instanceGroup) {
            com.sequenceiq.cloudbreak.api.model.InstanceStatus newStatus = newStatusByGroupType.get(group.getInstanceGroupType());
            if (newStatus != null) {
                for (InstanceMetaData instanceMetaData : group.getInstanceMetaData()) {
                    instanceMetaData.setInstanceStatus(newStatus);
                    instanceMetaDataRepository.save(instanceMetaData);
                }
            }
        }
    }

    public void updateInstanceStatus(Set<InstanceGroup> instanceGroup, com.sequenceiq.cloudbreak.api.model.InstanceStatus newStatus,
            Set<String> candidateAddresses) {
        for (InstanceGroup group : instanceGroup) {
            for (InstanceMetaData instanceMetaData : group.getInstanceMetaData()) {
                if (candidateAddresses.contains(instanceMetaData.getDiscoveryFQDN())) {
                    instanceMetaData.setInstanceStatus(newStatus);
                    instanceMetaDataRepository.save(instanceMetaData);
                }
            }
        }
    }

    public void saveInstanceRequests(Stack stack, List<Group> groups) {
        Set<InstanceGroup> instanceGroups = stack.getInstanceGroups();
        for (Group group : groups) {
            InstanceGroup instanceGroup = getInstanceGroup(instanceGroups, group.getName());
            for (CloudInstance cloudInstance : group.getInstances()) {
                InstanceTemplate instanceTemplate = cloudInstance.getTemplate();
                if (InstanceStatus.CREATE_REQUESTED == instanceTemplate.getStatus()) {
                    InstanceMetaData instanceMetaData = new InstanceMetaData();
                    instanceMetaData.setPrivateId(instanceTemplate.getPrivateId());
                    instanceMetaData.setInstanceStatus(com.sequenceiq.cloudbreak.api.model.InstanceStatus.REQUESTED);
                    instanceMetaData.setInstanceGroup(instanceGroup);
                    instanceMetaDataRepository.save(instanceMetaData);
                }
            }
        }
    }

    public void deleteInstanceRequest(long stackId, long privateId) {
        Set<InstanceMetaData> instanceMetaData = instanceMetaDataRepository.findAllInStack(stackId);
        for (InstanceMetaData metaData : instanceMetaData) {
            if (metaData.getPrivateId() == privateId) {
                instanceMetaDataRepository.delete(metaData);
                break;
            }
        }
    }

    private InstanceGroup getInstanceGroup(Set<InstanceGroup> instanceGroups, String groupName) {
        for (InstanceGroup instanceGroup : instanceGroups) {
            if (groupName.equalsIgnoreCase(instanceGroup.getGroupName())) {
                return instanceGroup;
            }
        }
        return null;
    }
}
