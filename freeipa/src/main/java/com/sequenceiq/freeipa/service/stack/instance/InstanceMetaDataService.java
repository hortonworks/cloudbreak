package com.sequenceiq.freeipa.service.stack.instance;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.projection.StackAuthenticationView;
import com.sequenceiq.freeipa.repository.InstanceMetaDataRepository;

@Service
public class InstanceMetaDataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceMetaDataService.class);

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    public void saveInstanceRequests(Stack stack, List<Group> groups) {
        Set<InstanceGroup> instanceGroups = stack.getInstanceGroups();
        for (Group group : groups) {
            InstanceGroup instanceGroup = getInstanceGroup(instanceGroups, group.getName());
            List<InstanceMetaData> existingInGroup = instanceMetaDataRepository.findAllByInstanceGroupAndInstanceStatus(instanceGroup,
                    com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus.REQUESTED);
            for (CloudInstance cloudInstance : group.getInstances()) {
                InstanceTemplate instanceTemplate = cloudInstance.getTemplate();
                boolean exists = existingInGroup.stream().anyMatch(i -> i.getPrivateId().equals(instanceTemplate.getPrivateId()));
                if (InstanceStatus.CREATE_REQUESTED == instanceTemplate.getStatus() && !exists) {
                    InstanceMetaData instanceMetaData = new InstanceMetaData();
                    instanceMetaData.setPrivateId(instanceTemplate.getPrivateId());
                    instanceMetaData.setInstanceStatus(com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus.REQUESTED);
                    instanceMetaData.setInstanceGroup(instanceGroup);
                    instanceMetaDataRepository.save(instanceMetaData);
                }
            }
        }
    }

    public void updateStatus(Stack stack, List<String> instanceIds, com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus status) {
        Set<InstanceMetaData> allInstances = instanceMetaDataRepository.findAllInStack(stack.getId());
        allInstances.stream().filter(im -> instanceIds.contains(im.getInstanceId()))
                .forEach(im -> {
                    im.setInstanceStatus(status);
                    instanceMetaDataRepository.save(im);
                    LOGGER.info("Updated instanceId {} status to {}.", im.getInstanceId(), status);
                });
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

    public Set<InstanceMetaData> findNotTerminatedForStack(Long stackId) {
        return instanceMetaDataRepository.findNotTerminatedForStack(stackId);
    }

    public InstanceMetaData save(InstanceMetaData instanceMetaData) {
        return instanceMetaDataRepository.save(instanceMetaData);
    }

    public Set<InstanceMetaData> getByInstanceId(String instanceId) {
        return instanceMetaDataRepository.findAllByInstanceIdIn(Set.of(instanceId));
    }

    public Optional<InstanceMetaData> getById(Long id) {
        return instanceMetaDataRepository.findById(id);
    }

    private InstanceGroup getInstanceGroup(Iterable<InstanceGroup> instanceGroups, String groupName) {
        for (InstanceGroup instanceGroup : instanceGroups) {
            if (groupName.equalsIgnoreCase(instanceGroup.getGroupName())) {
                return instanceGroup;
            }
        }
        return null;
    }

    public Iterable<InstanceMetaData> saveAll(Iterable<InstanceMetaData> allInstanceMetaData) {
        return instanceMetaDataRepository.saveAll(allInstanceMetaData);
    }

    public Optional<StackAuthenticationView> getStackAuthenticationViewByInstanceMetaDataId(Long instanceId) {
        return instanceMetaDataRepository.getStackAuthenticationViewByInstanceMetaDataId(instanceId);
    }
}
