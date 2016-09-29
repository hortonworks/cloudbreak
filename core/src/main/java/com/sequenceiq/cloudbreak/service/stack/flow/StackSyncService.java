package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.CREATE_FAILED;
import static com.sequenceiq.cloudbreak.api.model.Status.DELETE_FAILED;
import static com.sequenceiq.cloudbreak.api.model.Status.STOPPED;
import static com.sequenceiq.cloudbreak.api.model.Status.WAIT_FOR_SYNC;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.InstanceStatus;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.ResourceRepository;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterConnector;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariDecommissioner;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderMetadataAdapter;

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
    private AmbariDecommissioner ambariDecommissioner;
    @Inject
    private ServiceProviderMetadataAdapter metadata;
    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    public void updateInstances(Stack stack, List<InstanceMetaData> instanceMetaDataList, List<CloudVmInstanceStatus> instanceStatuses,
            boolean stackStatusUpdateEnabled) {
        Map<InstanceSyncState, Integer> counts = initInstanceStateCounts();
        for (final InstanceMetaData metaData : instanceMetaDataList) {
            Optional<CloudVmInstanceStatus> status = instanceStatuses.stream()
                    .filter(is -> is != null && is.getCloudInstance().getInstanceId() != null
                            && is.getCloudInstance().getInstanceId().equals(metaData.getInstanceId()))
                    .findFirst();

            InstanceSyncState state = !status.isPresent() ? InstanceSyncState.DELETED : transform(status.get().getStatus());
            syncInstanceStatusByState(stack, counts, metaData, state);
        }

        handleSyncResult(stack, counts, stackStatusUpdateEnabled);
    }

    public void sync(Long stackId, boolean stackStatusUpdateEnabled) {
        Stack stack = stackService.getById(stackId);
        if (stack.isStackInDeletionPhase() || stack.isModificationInProgress()) {
            LOGGER.warn("Stack could not be synchronized in {} state!", stack.getStatus());
        } else {
            sync(stack, stackStatusUpdateEnabled);
        }
    }

    private void sync(Stack stack, boolean stackStatusUpdateEnabled) {
        Long stackId = stack.getId();
        Set<InstanceMetaData> instances = instanceMetaDataRepository.findNotTerminatedForStack(stackId);
        Map<InstanceSyncState, Integer> instanceStateCounts = initInstanceStateCounts();
        for (InstanceMetaData instance : instances) {
            InstanceGroup instanceGroup = instance.getInstanceGroup();
            try {
                InstanceSyncState state = metadata.getState(stack, instanceGroup, instance.getInstanceId());
                syncInstanceStatusByState(stack, instanceStateCounts, instance, state);
            } catch (CloudConnectorException e) {
                LOGGER.warn(e.getMessage(), e);
                eventService.fireCloudbreakEvent(stackId, AVAILABLE.name(),
                        cloudbreakMessagesService.getMessage(Msg.STACK_SYNC_INSTANCE_STATUS_RETRIEVAL_FAILED.code(),
                                Collections.singletonList(instance.getInstanceId())));
                instanceStateCounts.put(InstanceSyncState.UNKNOWN, instanceStateCounts.get(InstanceSyncState.UNKNOWN) + 1);
            }
        }
        handleSyncResult(stack, instanceStateCounts, stackStatusUpdateEnabled);
    }

    private void syncInstanceStatusByState(Stack stack, Map<InstanceSyncState, Integer> counts, InstanceMetaData metaData, InstanceSyncState state) {
        if (InstanceSyncState.DELETED.equals(state)) {
            syncDeletedInstance(stack, counts, metaData);
        } else if (InstanceSyncState.RUNNING.equals(state)) {
            syncRunningInstance(stack, counts, metaData);
        } else if (InstanceSyncState.STOPPED.equals(state)) {
            syncStoppedInstance(stack, counts, metaData);
        } else {
            counts.put(InstanceSyncState.IN_PROGRESS, counts.get(InstanceSyncState.IN_PROGRESS) + 1);
        }
    }

    private void syncStoppedInstance(Stack stack, Map<InstanceSyncState, Integer> instanceStateCounts, InstanceMetaData instance) {
        instanceStateCounts.put(InstanceSyncState.STOPPED, instanceStateCounts.get(InstanceSyncState.STOPPED) + 1);
        if (!instance.isTerminated() && !stack.isStopped()) {
            LOGGER.info("Instance '{}' is reported as stopped on the cloud provider, setting its state to STOPPED.", instance.getInstanceId());
            instance.setInstanceStatus(InstanceStatus.STOPPED);
            instanceMetaDataRepository.save(instance);
            eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(),
                    cloudbreakMessagesService.getMessage(Msg.STACK_SYNC_INSTANCE_UPDATED.code(), Arrays.asList(instance.getInstanceId(), "stopped")));
        }
    }

    private void syncRunningInstance(Stack stack, Map<InstanceSyncState, Integer> instanceStateCounts, InstanceMetaData instance) {
        instanceStateCounts.put(InstanceSyncState.RUNNING, instanceStateCounts.get(InstanceSyncState.RUNNING) + 1);
        if (stack.getStatus() == WAIT_FOR_SYNC && instance.isCreated()) {
            LOGGER.info("Instance '{}' is reported as created on the cloud provider but not member of the cluster, setting its state to FAILED.",
                    instance.getInstanceId());
            instance.setInstanceStatus(InstanceStatus.FAILED);
            instanceMetaDataRepository.save(instance);
            eventService.fireCloudbreakEvent(stack.getId(), CREATE_FAILED.name(),
                    cloudbreakMessagesService.getMessage(Msg.STACK_SYNC_INSTANCE_FAILED.code(), Collections.singletonList(instance.getDiscoveryFQDN())));
        } else if (!instance.isRunning() && !instance.isDecommissioned() && !instance.isCreated() && !instance.isFailed()) {
            LOGGER.info("Instance '{}' is reported as running on the cloud provider, updating metadata.", instance.getInstanceId());
            updateMetaDataToRunning(stack.getId(), stack.getCluster(), instance);
        }
    }

    private void syncDeletedInstance(Stack stack, Map<InstanceSyncState, Integer> instanceStateCounts, InstanceMetaData instance) {
        deleteHostFromCluster(stack, instance);
        if (!instance.isTerminated()) {
            instanceStateCounts.put(InstanceSyncState.DELETED, instanceStateCounts.get(InstanceSyncState.DELETED) + 1);
            LOGGER.info("Instance '{}' is reported as deleted on the cloud provider, setting its state to TERMINATED.", instance.getInstanceId());
            deleteResourceIfNeeded(stack, instance);
            updateMetaDataToTerminated(stack, instance);
        }
    }

    private void deleteResourceIfNeeded(Stack stack, InstanceMetaData instance) {
        Resource resource = resourceRepository.findByStackIdAndResourceNameOrReference(stack.getId(), instance.getInstanceId());
        if (resource != null) {
            resourceRepository.delete(resource);
        }
    }

    private void handleSyncResult(Stack stack, Map<InstanceSyncState, Integer> instanceStateCounts, boolean stackStatusUpdateEnabled) {
        Set<InstanceMetaData> instances = instanceMetaDataRepository.findNotTerminatedForStack(stack.getId());
        if (instanceStateCounts.get(InstanceSyncState.UNKNOWN) > 0) {
            eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(),
                    cloudbreakMessagesService.getMessage(Msg.STACK_SYNC_INSTANCE_STATUS_COULDNT_DETERMINE.code()));
        } else if (instanceStateCounts.get(InstanceSyncState.IN_PROGRESS) > 0) {
            eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(),
                    cloudbreakMessagesService.getMessage(Msg.STACK_SYNC_INSTANCE_OPERATION_IN_PROGRESS.code()));
        } else if (instanceStateCounts.get(InstanceSyncState.RUNNING) > 0 && instanceStateCounts.get(InstanceSyncState.STOPPED) > 0) {
            eventService.fireCloudbreakEvent(stack.getId(), STOPPED.name(),
                    cloudbreakMessagesService.getMessage(Msg.STACK_SYNC_INSTANCE_STOPPED_ON_PROVIDER.code()));
        } else if (instanceStateCounts.get(InstanceSyncState.RUNNING) > 0) {
            updateStackStatusIfEnabled(stack.getId(), AVAILABLE, SYNC_STATUS_REASON, stackStatusUpdateEnabled);
            eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(),
                    cloudbreakMessagesService.getMessage(Msg.STACK_SYNC_INSTANCE_STATE_SYNCED.code()));
        } else if (instanceStateCounts.get(InstanceSyncState.STOPPED).equals(instances.size())) {
            updateStackStatusIfEnabled(stack.getId(), STOPPED, SYNC_STATUS_REASON, stackStatusUpdateEnabled);
            eventService.fireCloudbreakEvent(stack.getId(), STOPPED.name(),
                    cloudbreakMessagesService.getMessage(Msg.STACK_SYNC_INSTANCE_STATE_SYNCED.code()));
        } else {
            updateStackStatusIfEnabled(stack.getId(), DELETE_FAILED, SYNC_STATUS_REASON, stackStatusUpdateEnabled);
            eventService.fireCloudbreakEvent(stack.getId(), DELETE_FAILED.name(),
                    cloudbreakMessagesService.getMessage(Msg.STACK_SYNC_INSTANCE_STATE_SYNCED.code()));
        }
    }

    private void updateStackStatusIfEnabled(Long stackId, Status status, String statusReason, boolean stackStatusUpdateEnabled) {
        if (stackStatusUpdateEnabled) {
            stackUpdater.updateStackStatus(stackId, status, statusReason);
        }
    }

    private InstanceSyncState transform(com.sequenceiq.cloudbreak.cloud.model.InstanceStatus instanceStatus) {
        switch (instanceStatus) {
            case IN_PROGRESS:
                return InstanceSyncState.IN_PROGRESS;
            case STARTED:
                return InstanceSyncState.RUNNING;
            case STOPPED:
                return InstanceSyncState.STOPPED;
            case CREATED:
                return InstanceSyncState.RUNNING;
            case FAILED:
                return InstanceSyncState.DELETED;
            case TERMINATED:
                return InstanceSyncState.DELETED;
            default:
                return InstanceSyncState.UNKNOWN;
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
                    if (instanceMetaData.getInstanceStatus() != InstanceStatus.TERMINATED) {
                        throw new NotFoundException(String.format("Host not found with id '%s'", instanceMetaData.getDiscoveryFQDN()));
                    }
                } else {
                    if (ambariClusterConnector.isAmbariAvailable(stack)) {
                        if (ambariDecommissioner.deleteHostFromAmbari(stack, hostMetadata)) {
                            hostMetadataRepository.delete(hostMetadata.getId());
                            eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(),
                                    cloudbreakMessagesService.getMessage(Msg.STACK_SYNC_HOST_DELETED.code(),
                                            Collections.singletonList(instanceMetaData.getDiscoveryFQDN())));
                        } else {
                            eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(),
                                    cloudbreakMessagesService.getMessage(Msg.STACK_SYNC_INSTANCE_REMOVAL_FAILED.code(),
                                            Collections.singletonList(instanceMetaData.getDiscoveryFQDN())));
                        }
                    } else {
                        hostMetadata.setHostMetadataState(HostMetadataState.UNHEALTHY);
                        hostMetadataRepository.save(hostMetadata);
                        eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(),
                                cloudbreakMessagesService.getMessage(Msg.STACK_SYNC_HOST_UPDATED.code(),
                                        Arrays.asList(instanceMetaData.getDiscoveryFQDN(), HostMetadataState.UNHEALTHY.name())));
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Host cannot be deleted from cluster: ", e);
            eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(),
                    cloudbreakMessagesService.getMessage(Msg.STACK_SYNC_INSTANCE_TERMINATED.code(),
                            Collections.singletonList(instanceMetaData.getDiscoveryFQDN())));
        }
    }

    private void updateMetaDataToTerminated(Stack stack, InstanceMetaData instanceMetaData) {
        InstanceGroup instanceGroup = instanceMetaData.getInstanceGroup();
        List<InstanceMetaData> aliveInstancesInInstanceGroup = instanceMetaDataRepository.findAliveInstancesInInstanceGroup(instanceGroup.getId());
        instanceGroup.setNodeCount(aliveInstancesInInstanceGroup.size() - 1);
        long timeInMillis = Calendar.getInstance().getTimeInMillis();
        instanceMetaData.setTerminationDate(timeInMillis);
        instanceMetaData.setInstanceStatus(InstanceStatus.TERMINATED);
        instanceMetaDataRepository.save(instanceMetaData);
        instanceGroupRepository.save(instanceGroup);
        String name;
        if (instanceMetaData.getDiscoveryFQDN() == null) {
            name = instanceMetaData.getInstanceId();
        } else {
            name = String.format("%s (%s)", instanceMetaData.getInstanceId(), instanceMetaData.getDiscoveryFQDN());
        }

        eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(),
                cloudbreakMessagesService.getMessage(Msg.STACK_SYNC_INSTANCE_DELETED_CBMETADATA.code(),
                        Collections.singletonList(name)));
    }

    private void updateMetaDataToRunning(Long stackId, Cluster cluster, InstanceMetaData instanceMetaData) {
        InstanceGroup instanceGroup = instanceMetaData.getInstanceGroup();
        if (InstanceStatus.TERMINATED.equals(instanceMetaData.getInstanceStatus())) {
            instanceGroup.setNodeCount(instanceGroup.getNodeCount() + 1);
        }
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
                cloudbreakMessagesService.getMessage(Msg.STACK_SYNC_INSTANCE_UPDATED.code(), Arrays.asList(instanceMetaData.getDiscoveryFQDN(), "running")));
    }

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
        STACK_SYNC_INSTANCE_UPDATED("stack.sync.instance.updated"),
        STACK_SYNC_INSTANCE_FAILED("stack.sync.instance.failed");

        private String code;

        Msg(String msgCode) {
            code = msgCode;
        }

        public String code() {
            return code;
        }
    }


}