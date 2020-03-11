package com.sequenceiq.cloudbreak.service.stack;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.domain.projection.InstanceMetaDataGroupView;
import com.sequenceiq.cloudbreak.domain.projection.StackInstanceCount;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;

@Service
public class InstanceMetaDataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceMetaDataService.class);

    @Inject
    private InstanceMetaDataRepository repository;

    public void updateInstanceStatus(InstanceMetaData instanceMetaData, com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus newStatus) {
        updateInstanceStatus(instanceMetaData, newStatus, null);
    }

    public void updateInstanceStatus(InstanceMetaData instanceMetaData, com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus newStatus,
            String statusReason) {
        instanceMetaData.setInstanceStatus(newStatus);
        instanceMetaData.setStatusReason(statusReason);
        repository.save(instanceMetaData);
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
                    repository.save(instanceMetaData);
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
            List<InstanceMetaData> existingInGroup = repository.findAllByInstanceGroupAndInstanceStatus(instanceGroup,
                    com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.REQUESTED);
            for (CloudInstance cloudInstance : group.getInstances()) {
                InstanceTemplate instanceTemplate = cloudInstance.getTemplate();
                boolean exists = existingInGroup.stream().anyMatch(i -> i.getPrivateId().equals(instanceTemplate.getPrivateId()));
                if (InstanceStatus.CREATE_REQUESTED == instanceTemplate.getStatus() && !exists) {
                    InstanceMetaData instanceMetaData = new InstanceMetaData();
                    instanceMetaData.setPrivateId(instanceTemplate.getPrivateId());
                    instanceMetaData.setInstanceStatus(com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.REQUESTED);
                    instanceMetaData.setInstanceGroup(instanceGroup);
                    repository.save(instanceMetaData);
                }
            }
        }
    }

    public void deleteInstanceRequest(Long stackId, Long privateId) {
        Set<InstanceMetaData> instanceMetaData = repository.findAllInStack(stackId);
        for (InstanceMetaData metaData : instanceMetaData) {
            if (metaData.getPrivateId().equals(privateId)) {
                repository.delete(metaData);
                break;
            }
        }
    }

    public Set<InstanceMetaData> unusedInstancesInInstanceGroupByName(Long stackId, String instanceGroupName) {
        return repository.findUnusedHostsInInstanceGroup(stackId, instanceGroupName);
    }

    public Set<InstanceMetaData> getAllInstanceMetadataByStackId(Long stackId) {
        return repository.findAllInStack(stackId);
    }

    private InstanceGroup getInstanceGroup(Iterable<InstanceGroup> instanceGroups, String groupName) {
        for (InstanceGroup instanceGroup : instanceGroups) {
            if (groupName.equalsIgnoreCase(instanceGroup.getGroupName())) {
                return instanceGroup;
            }
        }
        return null;
    }

    public Optional<InstanceMetaData> getPrimaryGatewayInstanceMetadata(long stackId) {
        try {
            return repository.getPrimaryGatewayInstanceMetadata(stackId);
        } catch (AccessDeniedException ignore) {
            LOGGER.debug("No primary gateway for stack [{}]", stackId);
            return Optional.empty();
        }
    }

    public InstanceMetaData save(InstanceMetaData instanceMetaData) {
        return repository.save(instanceMetaData);
    }

    public Set<InstanceMetaData> findAllInStack(Long stackId) {
        return repository.findAllInStack(stackId);
    }

    public Set<InstanceMetaData> findNotTerminatedForStack(Long stackId) {
        return repository.findNotTerminatedForStack(stackId);
    }

    public Set<InstanceMetaData> findNotTerminatedForStackWithoutInstanceGroups(Long stackId) {
        return repository.findNotTerminatedForStackWithoutInstanceGroups(stackId);
    }

    public Optional<InstanceMetaData> findByStackIdAndInstanceId(Long stackId, String instanceId) {
        return repository.findByStackIdAndInstanceId(stackId, instanceId);
    }

    public Optional<InstanceMetaData> findHostInStack(Long stackId, String hostName) {
        return repository.findHostInStack(stackId, hostName);
    }

    public Optional<InstanceMetaDataGroupView> findInstanceGroupViewInClusterByName(Long stackId, String hostName) {
        return repository.findInstanceGroupViewInClusterByName(stackId, hostName);
    }

    public Optional<InstanceMetaData> findHostInStackWithoutInstanceGroup(Long stackId, String hostName) {
        return repository.findHostInStackWithoutInstanceGroup(stackId, hostName);
    }

    public Set<InstanceMetaData> findUnusedHostsInInstanceGroup(Long instanceGroupId) {
        return repository.findUnusedHostsInInstanceGroup(instanceGroupId);
    }

    public void delete(InstanceMetaData instanceMetaData) {
        repository.delete(instanceMetaData);
    }

    public Optional<InstanceMetaData> findNotTerminatedByPrivateAddress(Long stackId, String privateAddress) {
        return repository.findNotTerminatedByPrivateAddress(stackId, privateAddress);
    }

    public List<InstanceMetaData> findAliveInstancesInInstanceGroup(Long instanceGroupId) {
        return repository.findAliveInstancesInInstanceGroup(instanceGroupId);
    }

    public Iterable<InstanceMetaData> saveAll(Iterable<InstanceMetaData> instanceMetaData) {
        return repository.saveAll(instanceMetaData);
    }

    public Optional<String> getPrimaryGatewayDiscoveryFQDNByInstanceGroup(Long stackId, Long instanceGroupId) {
        return repository.getPrimaryGatewayDiscoveryFQDNByInstanceGroup(stackId, instanceGroupId);
    }

    public Optional<String> getServerCertByStackId(Long stackId) {
        return repository.getServerCertByStackId(stackId);
    }

    public Optional<InstanceMetaData> findById(Long id) {
        return repository.findById(id);
    }

    public Set<InstanceMetaData> findRemovableInstances(Long stackId, String groupName) {
        return repository.findRemovableInstances(stackId, groupName);
    }

    public Set<StackInstanceCount> countByWorkspaceId(Long workspaceId, String environmentCrn, List<StackType> stackTypes) {
        return repository.countByWorkspaceId(workspaceId, environmentCrn, stackTypes);
    }

    public List<InstanceMetaData> findAllByInstanceGroupAndInstanceStatus(InstanceGroup instanceGroup,
            com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus status) {
        return repository.findAllByInstanceGroupAndInstanceStatus(instanceGroup, status);
    }

    public Optional<InstanceMetaData> findByHostname(Long stackId, String hostName) {
        return repository.findHostInStack(stackId, hostName);
    }
}
