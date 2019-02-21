package com.sequenceiq.cloudbreak.service.stack;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;

@Service
public class InstanceMetaDataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceMetaDataService.class);

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    public void updateInstanceStatus(Iterable<InstanceGroup> instanceGroup,
            Map<InstanceGroupType, com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus> newStatusByGroupType) {
        for (InstanceGroup group : instanceGroup) {
            com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus newStatus = newStatusByGroupType.get(group.getInstanceGroupType());
            if (newStatus != null) {
                for (InstanceMetaData instanceMetaData : group.getNotDeletedInstanceMetaDataSet()) {
                    instanceMetaData.setInstanceStatus(newStatus);
                    instanceMetaDataRepository.save(instanceMetaData);
                }
            }
        }
    }

    public void updateInstanceStatus(Iterable<InstanceGroup> instanceGroup, com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus newStatus,
            Collection<String> candidateAddresses) {
        for (InstanceGroup group : instanceGroup) {
            for (InstanceMetaData instanceMetaData : group.getNotDeletedInstanceMetaDataSet()) {
                if (candidateAddresses.contains(instanceMetaData.getDiscoveryFQDN())) {
                    instanceMetaData.setInstanceStatus(newStatus);
                    instanceMetaDataRepository.save(instanceMetaData);
                }
            }
        }
    }

    public Stack saveInstanceAndGetUpdatedStack(Stack stack, List<CloudInstance> cloudInstances, boolean save) {
        for (CloudInstance cloudInstance : cloudInstances) {
            InstanceGroup instanceGroup = getInstanceGroup(stack.getInstanceGroups(), cloudInstance.getTemplate().getGroupName());
            if (instanceGroup != null) {
                InstanceMetaData instanceMetaData = new InstanceMetaData();
                instanceMetaData.setPrivateId(cloudInstance.getTemplate().getPrivateId());
                instanceMetaData.setInstanceStatus(com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.REQUESTED);
                instanceMetaData.setInstanceGroup(instanceGroup);
                if (save) {
                    instanceMetaDataRepository.save(instanceMetaData);
                }
                instanceGroup.getInstanceMetaDataSet().add(instanceMetaData);
            }
        }
        return stack;
    }

    public void saveInstanceRequests(Stack stack, List<Group> groups) {
        Set<InstanceGroup> instanceGroups = stack.getInstanceGroups();
        for (Group group : groups) {
            InstanceGroup instanceGroup = getInstanceGroup(instanceGroups, group.getName());
            List<InstanceMetaData> existingInGroup = instanceMetaDataRepository.findAllByInstanceGroupAndInstanceStatus(instanceGroup,
                    com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.REQUESTED);
            for (CloudInstance cloudInstance : group.getInstances()) {
                InstanceTemplate instanceTemplate = cloudInstance.getTemplate();
                boolean exists = existingInGroup.stream().anyMatch(i -> i.getPrivateId().equals(instanceTemplate.getPrivateId()));
                if (InstanceStatus.CREATE_REQUESTED == instanceTemplate.getStatus() && !exists) {
                    InstanceMetaData instanceMetaData = new InstanceMetaData();
                    instanceMetaData.setPrivateId(instanceTemplate.getPrivateId());
                    instanceMetaData.setInstanceStatus(com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.REQUESTED);
                    instanceMetaData.setInstanceGroup(instanceGroup);
                    instanceMetaDataRepository.save(instanceMetaData);
                }
            }
        }
    }

    public void deleteInstanceRequest(Long stackId, Long privateId) {
        Set<InstanceMetaData> instanceMetaData = instanceMetaDataRepository.findAllInStack(stackId);
        for (InstanceMetaData metaData : instanceMetaData) {
            if (metaData.getPrivateId().equals(privateId)) {
                instanceMetaDataRepository.delete(metaData);
                break;
            }
        }
    }

    public Set<InstanceMetaData> unusedInstancesInInstanceGroupByName(Long stackId, String instanceGroupName) {
        return instanceMetaDataRepository.findUnusedHostsInInstanceGroup(stackId, instanceGroupName);
    }

    private InstanceGroup getInstanceGroup(Iterable<InstanceGroup> instanceGroups, String groupName) {
        for (InstanceGroup instanceGroup : instanceGroups) {
            if (groupName.equalsIgnoreCase(instanceGroup.getGroupName())) {
                return instanceGroup;
            }
        }
        return null;
    }

    public InstanceMetaData getPrimaryGatewayInstanceMetadata(long stackId) {
        try {
            return instanceMetaDataRepository.getPrimaryGatewayInstanceMetadata(stackId);
        } catch (AccessDeniedException ignore) {
            LOGGER.debug("No primary gateway for stack [{}]", stackId);
            return null;
        }
    }

    public InstanceMetaData pureSave(InstanceMetaData instanceMetaData) {
        return instanceMetaDataRepository.save(instanceMetaData);
    }

    public Set<InstanceMetaData> findAllInStack(Long stackId) {
        return instanceMetaDataRepository.findAllInStack(stackId);
    }
}
