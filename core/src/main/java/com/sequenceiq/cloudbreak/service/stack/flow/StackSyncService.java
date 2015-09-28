package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.common.type.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.common.type.Status.DELETE_FAILED;
import static com.sequenceiq.cloudbreak.common.type.Status.STOPPED;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.common.type.InstanceStatus;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.ResourceRepository;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.CloudPlatformResolver;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterConnector;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.connector.MetadataSetup;

@Service
public class StackSyncService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackSyncService.class);

    private static final String SYNC_STATUS_REASON = "Synced instance states with the cloud provider.";

    @Inject
    private StackService stackService;
    @Inject
    private StackUpdater stackUpdater;
    @Inject
    private CloudbreakEventService eventService;
    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;
    @Inject
    private InstanceGroupRepository instanceGroupRepository;
    @Inject
    private HostMetadataRepository hostMetadataRepository;
    @Inject
    private ResourceRepository resourceRepository;
    @Inject
    private AmbariClusterConnector ambariClusterConnector;
    @Inject
    private CloudPlatformResolver platformResolver;
    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    private enum Msg {
        STACK_SYNC_INSTANCE_STATUS_RETRIEVAL_FAILED("stack.sync.instance.status.retrieval.failed"),
        STACK_SYNC_INSTANCE_STATUS_COULDNT_DETERMINE("stack.sync.instance.status.couldnt.determine"),
        STACK_SYNC_INSTANCE_OPERATION_IN_PROGRESS("stack.sync.instance.operation.in.progress"),
        STACK_SYNC_INSTANCE_STOPPED_ON_PROVIDER("stack.sync.instance.stopped.on.provider"),
        STACK_SYNC_INSTANCE_STATE_SYNCED("stack.sync.instance.state.synced"),
        STACK_SYNC_HOST_DELETED("stack.sync.host.deleted"),
        STACK_SYNC_INSTANCE_REMOVAL_FAILED("stack.sync.instance.removal.failed"),
        STACK_SYNC_HOST_UPDATED("stack.sync.host.updated"),
        STACK_SYNC_INSTANCE_TERMINATED("stack.sync.instance.terminated"),
        STACK_SYNC_INSTANCE_DELETED_CBMETADATA("stack.sync.instance.deleted.cbmetadata"),
        STACK_SYNC_INSTANCE_RUNNING("stack.sync.instance.running");

        private String code;

        Msg(String msgCode) {
            code = msgCode;
        }

        public String code() {
            return code;
        }
    }

    public void sync(Long stackId) {
        Stack stack = stackService.getById(stackId);
        if (stack.isStackInDeletionPhase() || stack.isModificationInProgress()) {
            LOGGER.warn("Stack could not be synchronized in {} state!", stack.getStatus());
        } else {
            sync(stack);
        }
    }

    private void sync(Stack stack) {
        Long stackId = stack.getId();
        Set<InstanceMetaData> instances = instanceMetaDataRepository.findAllInStack(stackId);
        Map<InstanceSyncState, Integer> instanceStateCounts = initInstanceStateCounts();
        for (InstanceMetaData instance : instances) {
            InstanceGroup instanceGroup = instance.getInstanceGroup();
            try {
                MetadataSetup metadataSetup = platformResolver.metadata(stack.cloudPlatform());
                InstanceSyncState state = metadataSetup.getState(stack, instanceGroup, instance.getInstanceId());
                ResourceType instanceResourceType = metadataSetup.getInstanceResourceType();
                if (InstanceSyncState.DELETED.equals(state) && !instance.isTerminated()) {
                    syncDeletedInstance(stack, stackId, instanceStateCounts, instance, instanceGroup, instanceResourceType);
                } else if (InstanceSyncState.RUNNING.equals(state)) {
                    syncRunningInstance(stack, stackId, instanceStateCounts, instance, instanceGroup);
                } else if (InstanceSyncState.STOPPED.equals(state)) {
                    syncStoppedInstance(stack, stackId, instanceStateCounts, instance, instanceGroup, instanceResourceType);
                } else {
                    instanceStateCounts.put(InstanceSyncState.IN_PROGRESS, instanceStateCounts.get(InstanceSyncState.IN_PROGRESS) + 1);
                }
            } catch (CloudConnectorException e) {
                LOGGER.warn(e.getMessage(), e);
                eventService.fireCloudbreakEvent(stackId, AVAILABLE.name(),
                        cloudbreakMessagesService.getMessage(Msg.STACK_SYNC_INSTANCE_STATUS_RETRIEVAL_FAILED.code(), Arrays.asList(instance.getInstanceId())));
                instanceStateCounts.put(InstanceSyncState.UNKNOWN, instanceStateCounts.get(InstanceSyncState.UNKNOWN) + 1);
            }
        }
        handleSyncResult(stack, instanceStateCounts);
    }

    private void syncStoppedInstance(Stack stack, Long stackId, Map<InstanceSyncState, Integer> instanceStateCounts, InstanceMetaData instance,
            InstanceGroup instanceGroup, ResourceType instanceResourceType) {
        instanceStateCounts.put(InstanceSyncState.STOPPED, instanceStateCounts.get(InstanceSyncState.STOPPED) + 1);
        if (!instance.isTerminated() && !stack.isStopped()) {
            LOGGER.info("Instance '{}' is reported as stopped on the cloud provider, setting its state to STOPPED.", instance.getInstanceId());
            deleteResourceIfNeeded(stackId, instance, instanceResourceType);
            updateMetaDataToTerminated(stackId, instance, instanceGroup);
        }
    }

    private void syncRunningInstance(Stack stack, Long stackId, Map<InstanceSyncState, Integer> instanceStateCounts, InstanceMetaData instance,
            InstanceGroup instanceGroup) {
        instanceStateCounts.put(InstanceSyncState.RUNNING, instanceStateCounts.get(InstanceSyncState.RUNNING) + 1);
        if (!instance.isRunning() && !instance.isDecommissioned()) {
            LOGGER.info("Instance '{}' is reported as running on the cloud provider, updating metadata.", instance.getInstanceId());
            createResourceIfNeeded(stack, instance, instanceGroup);
            updateMetaDataToRunning(stackId, stack.getCluster(), instance, instanceGroup);
        }
    }

    private void syncDeletedInstance(Stack stack, Long stackId, Map<InstanceSyncState, Integer> instanceStateCounts, InstanceMetaData instance,
            InstanceGroup instanceGroup, ResourceType instanceResourceType) {
        instanceStateCounts.put(InstanceSyncState.DELETED, instanceStateCounts.get(InstanceSyncState.DELETED) + 1);
        deleteHostFromCluster(stack, instance);
        if (!instance.isTerminated()) {
            LOGGER.info("Instance '{}' is reported as deleted on the cloud provider, setting its state to TERMINATED.", instance.getInstanceId());
            deleteResourceIfNeeded(stackId, instance, instanceResourceType);
            updateMetaDataToTerminated(stackId, instance, instanceGroup);
        }
    }

    private void createResourceIfNeeded(Stack stack, InstanceMetaData instance, InstanceGroup instanceGroup) {
        ResourceType resourceType = platformResolver.metadata(stack.cloudPlatform()).getInstanceResourceType();
        if (resourceType != null) {
            Resource resource = new Resource(resourceType, instance.getInstanceId(), stack, instanceGroup.getGroupName());
            resourceRepository.save(resource);
        }
    }

    private void deleteResourceIfNeeded(Long stackId, InstanceMetaData instance, ResourceType instanceResourceType) {
        Resource resource = resourceRepository.findByStackIdAndNameAndType(stackId, instance.getInstanceId(), instanceResourceType);
        if (resource != null) {
            resourceRepository.delete(resource);
        }
    }

    private void handleSyncResult(Stack stack, Map<InstanceSyncState, Integer> instanceStateCounts) {
        if (instanceStateCounts.get(InstanceSyncState.UNKNOWN) > 0) {
            eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(),
                    cloudbreakMessagesService.getMessage(Msg.STACK_SYNC_INSTANCE_STATUS_COULDNT_DETERMINE.code()));
        } else if (instanceStateCounts.get(InstanceSyncState.IN_PROGRESS) > 0) {
            eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(),
                    cloudbreakMessagesService.getMessage(Msg.STACK_SYNC_INSTANCE_OPERATION_IN_PROGRESS.code()));
        } else if (instanceStateCounts.get(InstanceSyncState.RUNNING) > 0 && instanceStateCounts.get(InstanceSyncState.STOPPED) > 0) {
            eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(),
                    cloudbreakMessagesService.getMessage(Msg.STACK_SYNC_HOST_DELETED.code()));
        } else if (instanceStateCounts.get(InstanceSyncState.RUNNING) > 0) {
            stackUpdater.updateStackStatus(stack.getId(), AVAILABLE, SYNC_STATUS_REASON);
            eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(),
                    cloudbreakMessagesService.getMessage(Msg.STACK_SYNC_INSTANCE_STATE_SYNCED.code()));
        } else if (instanceStateCounts.get(InstanceSyncState.STOPPED) > 0) {
            stackUpdater.updateStackStatus(stack.getId(), STOPPED, SYNC_STATUS_REASON);
            eventService.fireCloudbreakEvent(stack.getId(), STOPPED.name(),
                    cloudbreakMessagesService.getMessage(Msg.STACK_SYNC_INSTANCE_STATE_SYNCED.code()));
        } else {
            stackUpdater.updateStackStatus(stack.getId(), DELETE_FAILED, SYNC_STATUS_REASON);
            eventService.fireCloudbreakEvent(stack.getId(), DELETE_FAILED.name(),
                    cloudbreakMessagesService.getMessage(Msg.STACK_SYNC_INSTANCE_STATE_SYNCED.code()));
        }
    }

    private Map<InstanceSyncState, Integer> initInstanceStateCounts() {
        Map<InstanceSyncState, Integer> instanceStates = new HashMap<>();
        instanceStates.put(InstanceSyncState.DELETED, 0);
        instanceStates.put(InstanceSyncState.STOPPED, 0);
        instanceStates.put(InstanceSyncState.RUNNING, 0);
        instanceStates.put(InstanceSyncState.IN_PROGRESS, 0);
        instanceStates.put(InstanceSyncState.UNKNOWN, 0);
        return instanceStates;
    }

    private void deleteHostFromCluster(Stack stack, InstanceMetaData instanceMetaData) {
        try {
            if (stack.getCluster() != null) {
                HostMetadata hostMetadata = hostMetadataRepository.findHostInClusterByName(stack.getCluster().getId(), instanceMetaData.getDiscoveryFQDN());
                if (hostMetadata == null) {
                    throw new NotFoundException(String.format("Host not found with id '%s'", instanceMetaData.getDiscoveryFQDN()));
                }
                if (ambariClusterConnector.isAmbariAvailable(stack)) {
                    if (ambariClusterConnector.deleteHostFromAmbari(stack, hostMetadata)) {
                        hostMetadataRepository.delete(hostMetadata.getId());
                        eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(),
                                cloudbreakMessagesService.getMessage(Msg.STACK_SYNC_HOST_DELETED.code(), Arrays.asList(instanceMetaData.getDiscoveryFQDN())));
                    } else {
                        eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(),
                                cloudbreakMessagesService.getMessage(Msg.STACK_SYNC_INSTANCE_REMOVAL_FAILED.code(),
                                        Arrays.asList(instanceMetaData.getDiscoveryFQDN())));
                    }
                } else {
                    hostMetadata.setHostMetadataState(HostMetadataState.UNHEALTHY);
                    hostMetadataRepository.save(hostMetadata);
                    eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(),
                            cloudbreakMessagesService.getMessage(Msg.STACK_SYNC_HOST_UPDATED.code(),
                                    Arrays.asList(instanceMetaData.getDiscoveryFQDN(), HostMetadataState.UNHEALTHY.name())));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Host cannot be deleted from cluster: ", e);
            eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(),
                    cloudbreakMessagesService.getMessage(Msg.STACK_SYNC_INSTANCE_TERMINATED.code(), Arrays.asList(instanceMetaData.getDiscoveryFQDN())));
        }
    }

    private void updateMetaDataToTerminated(Long stackId, InstanceMetaData instanceMetaData, InstanceGroup instanceGroup) {
        instanceGroup.setNodeCount(instanceGroup.getNodeCount() - 1);
        instanceMetaData.setInstanceStatus(InstanceStatus.TERMINATED);
        instanceMetaDataRepository.save(instanceMetaData);
        instanceGroupRepository.save(instanceGroup);
        eventService.fireCloudbreakEvent(stackId, AVAILABLE.name(),
                cloudbreakMessagesService.getMessage(Msg.STACK_SYNC_INSTANCE_DELETED_CBMETADATA.code(), Arrays.asList(instanceMetaData.getDiscoveryFQDN())));
    }

    private void updateMetaDataToRunning(Long stackId, Cluster cluster, InstanceMetaData instanceMetaData, InstanceGroup instanceGroup) {
        instanceGroup.setNodeCount(instanceGroup.getNodeCount() + 1);
        HostMetadata hostMetadata = hostMetadataRepository.findHostInClusterByName(cluster.getId(), instanceMetaData.getDiscoveryFQDN());
        if (hostMetadata != null) {
            LOGGER.info("Instance '{}' was found in the cluster metadata, setting it's state to REGISTERED.", instanceMetaData.getInstanceId());
            instanceMetaData.setInstanceStatus(InstanceStatus.REGISTERED);
        } else {
            LOGGER.info("Instance '{}' was not found in the cluster metadata, setting it's state to UNREGISTERED.", instanceMetaData.getInstanceId());
            instanceMetaData.setInstanceStatus(InstanceStatus.UNREGISTERED);
        }
        instanceMetaDataRepository.save(instanceMetaData);
        instanceGroupRepository.save(instanceGroup);
        eventService.fireCloudbreakEvent(stackId, AVAILABLE.name(),
                cloudbreakMessagesService.getMessage(Msg.STACK_SYNC_INSTANCE_RUNNING.code(), Arrays.asList(instanceMetaData.getDiscoveryFQDN())));
    }


}