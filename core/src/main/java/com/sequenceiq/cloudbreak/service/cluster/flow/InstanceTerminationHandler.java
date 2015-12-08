package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.type.InstanceStatus;
import com.sequenceiq.cloudbreak.common.type.Status;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.ResourceRepository;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderConnectorAdapter;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderMetadataAdapter;

@Service
public class InstanceTerminationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceTerminationHandler.class);

    @Inject
    private ResourceRepository resourceRepository;
    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;
    @Inject
    private InstanceGroupRepository instanceGroupRepository;
    @Inject
    private HostMetadataRepository hostMetadataRepository;
    @Inject
    private CloudbreakEventService eventService;
    @Inject
    private ServiceProviderConnectorAdapter connector;
    @Inject
    private ServiceProviderMetadataAdapter metadata;
    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;


    private enum Msg {
        STACK_INSTANCE_TERMINATE("stack.instance.terminate"),
        STACK_INSTANCE_DELETE("stack.instance.delete");

        private String code;

        Msg(String msgCode) {
            code = msgCode;
        }

        public String code() {
            return code;
        }
    }

    public void terminateInstance(Stack stack, InstanceMetaData instanceMetaData) {
        String message = cloudbreakMessagesService.getMessage(Msg.STACK_INSTANCE_TERMINATE.code(),
                Arrays.asList(instanceMetaData.getInstanceId()));
        LOGGER.info(message);
        eventService.fireCloudbreakEvent(stack.getId(), Status.UPDATE_IN_PROGRESS.name(), message);
        InstanceGroup ig = instanceGroupRepository.findOneByGroupNameInStack(stack.getId(), instanceMetaData.getInstanceGroup().getGroupName());
        ig.setNodeCount(ig.getNodeCount() - 1);
        instanceGroupRepository.save(ig);
        message = cloudbreakMessagesService.getMessage(Msg.STACK_INSTANCE_DELETE.code(),
                Arrays.asList(instanceMetaData.getInstanceId(), ig.getGroupName()));
        LOGGER.info(message);
        eventService.fireCloudbreakEvent(stack.getId(), Status.UPDATE_IN_PROGRESS.name(), message);
        deleteResourceAndDependencies(stack, instanceMetaData);
        deleteInstanceResourceFromDatabase(stack, instanceMetaData);
        instanceMetaData.setInstanceStatus(InstanceStatus.TERMINATED);
        instanceMetaDataRepository.save(instanceMetaData);
        LOGGER.info("InstanceMetadata [ id: {}, name: {} ] status set {}",
                instanceMetaData.getId(), instanceMetaData.getInstanceId(), instanceMetaData.getInstanceStatus());
    }

    private void deleteResourceAndDependencies(Stack stack, InstanceMetaData instanceMetaData) {
        LOGGER.info("Rolling back instance: {}", instanceMetaData.getInstanceId());
        Set<String> instanceIds = new HashSet<>();
        instanceIds.add(instanceMetaData.getInstanceId());
        connector.removeInstances(stack, instanceIds, instanceMetaData.getInstanceGroup().getGroupName());
        LOGGER.info("Instance [ id: {}, name: {} ] deleted", instanceMetaData.getId(), instanceMetaData.getInstanceId());
    }

    private void deleteInstanceResourceFromDatabase(Stack stack, InstanceMetaData instanceMetaData) {
        String instanceId = instanceMetaData.getInstanceId();
        Resource resource = resourceRepository.findByStackIdAndNameAndType(stack.getId(), instanceId,
                metadata.getInstanceResourceType());
        if (resource != null) {
            resourceRepository.delete(resource);
        } else {
            LOGGER.error("The terminated instance '{}' of stack '{}' could not be found in the database as resource!", instanceId, stack.getId());
        }
    }
}
