package com.sequenceiq.cloudbreak.service.stack;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private InstanceMetaDataRepository repository;

    public void updateInstanceStatus(Iterable<InstanceGroup> instanceGroup,
            Map<InstanceGroupType, com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus> newStatusByGroupType) {
        for (InstanceGroup group : instanceGroup) {
            com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus newStatus = newStatusByGroupType.get(group.getInstanceGroupType());
            if (newStatus != null) {
                for (InstanceMetaData instanceMetaData : group.getNotDeletedInstanceMetaDataSet()) {
                    instanceMetaData.setInstanceStatus(newStatus);
                    repository.save(instanceMetaData);
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
                    repository.save(instanceMetaData);
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

    public Optional<InstanceMetaData> findByStackIdAndInstanceId(Long stackId, String instanceId) {
        return repository.findByStackIdAndInstanceId(stackId, instanceId);
    }

    public Optional<InstanceMetaData> findHostInStack(Long stackId, String hostName) {
        return repository.findHostInStack(stackId, hostName);
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

    public List<InstanceMetaData> getPrimaryGatewayByInstanceGroup(Long stackId, Long instanceGroupId) {
        return repository.getPrimaryGatewayByInstanceGroup(stackId, instanceGroupId);
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

}
