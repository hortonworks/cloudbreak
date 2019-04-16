package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.CREATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOPPED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.WAIT_FOR_SYNC;
import static com.sequenceiq.cloudbreak.cloud.model.CloudInstance.INSTANCE_NAME;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.hostmetadata.HostMetadataService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
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
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private HostMetadataService hostMetadataService;

    @Inject
    private ServiceProviderMetadataAdapter metadata;

    @Inject
    private ImageService imageService;

    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    public void updateInstances(Stack stack, Iterable<InstanceMetaData> instanceMetaDataList, Collection<CloudVmInstanceStatus> instanceStatuses,
            boolean stackStatusUpdateEnabled) {
        try {
            Map<InstanceSyncState, Integer> counts = initInstanceStateCounts();
            Json imageJson = new Json(imageService.getImage(stack.getId()));
            for (InstanceMetaData metaData : instanceMetaDataList) {
                Optional<CloudVmInstanceStatus> status = instanceStatuses.stream()
                        .filter(is -> is != null && is.getCloudInstance().getInstanceId() != null
                                && is.getCloudInstance().getInstanceId().equals(metaData.getInstanceId()))
                        .findFirst();

                InstanceSyncState state;
                if (status.isPresent()) {
                    CloudVmInstanceStatus cloudVmInstanceStatus = status.get();
                    CloudInstance cloudInstance = cloudVmInstanceStatus.getCloudInstance();
                    state = InstanceSyncState.getInstanceSyncState(cloudVmInstanceStatus.getStatus());
                    syncInstance(metaData, cloudInstance, imageJson);
                } else {
                    state = InstanceSyncState.DELETED;
                }
                syncInstanceStatusByState(stack, counts, metaData, state);
            }
            handleSyncResult(stack, counts, stackStatusUpdateEnabled);
        } catch (CloudbreakImageNotFoundException | JsonProcessingException ex) {
            LOGGER.info("Error during stack sync:", ex);
            throw new CloudbreakServiceException("Stack sync failed", ex);
        }
    }

    public void sync(Long stackId, boolean stackStatusUpdateEnabled) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        if (stack.isStackInDeletionPhase() || stack.isModificationInProgress()) {
            LOGGER.debug("Stack could not be synchronized in {} state!", stack.getStatus());
        } else {
            sync(stack, stackStatusUpdateEnabled);
        }
    }

    private void sync(Stack stack, boolean stackStatusUpdateEnabled) {
        Long stackId = stack.getId();
        Set<InstanceMetaData> instances = instanceMetaDataService.findNotTerminatedForStack(stackId);
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

    private void syncInstance(InstanceMetaData instanceMetaData, CloudInstance cloudInstance, Json imageJson) {
        String instanceName = cloudInstance.getStringParameter(INSTANCE_NAME);
        instanceMetaData.setInstanceName(instanceName);
        if (instanceMetaData.getImage() == null) {
            instanceMetaData.setImage(imageJson);
        }
        instanceMetaDataService.save(instanceMetaData);
    }

    private void syncInstanceStatusByState(Stack stack, Map<InstanceSyncState, Integer> counts, InstanceMetaData metaData, InstanceSyncState state) {
        if (InstanceSyncState.DELETED.equals(state) || InstanceSyncState.DELETED_ON_PROVIDER_SIDE.equals(state)) {
            syncDeletedInstance(stack, counts, metaData);
        } else if (InstanceSyncState.RUNNING.equals(state)) {
            syncRunningInstance(stack, counts, metaData);
        } else if (InstanceSyncState.STOPPED.equals(state)) {
            syncStoppedInstance(stack, counts, metaData);
        } else {
            counts.put(state, counts.get(state) + 1);
        }
    }

    private void syncStoppedInstance(Stack stack, Map<InstanceSyncState, Integer> instanceStateCounts, InstanceMetaData instance) {
        instanceStateCounts.put(InstanceSyncState.STOPPED, instanceStateCounts.get(InstanceSyncState.STOPPED) + 1);
        if (!instance.isTerminated()) {
            LOGGER.debug("Instance '{}' is reported as stopped on the cloud provider, setting its state to STOPPED.", instance.getInstanceId());
            instance.setInstanceStatus(InstanceStatus.STOPPED);
            instanceMetaDataService.save(instance);
            eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(),
                    cloudbreakMessagesService.getMessage(Msg.STACK_SYNC_INSTANCE_UPDATED.code(), Arrays.asList(instance.getInstanceId(), "stopped")));
        }
    }

    private void syncRunningInstance(Stack stack, Map<InstanceSyncState, Integer> instanceStateCounts, InstanceMetaData instance) {
        instanceStateCounts.put(InstanceSyncState.RUNNING, instanceStateCounts.get(InstanceSyncState.RUNNING) + 1);
        if (stack.getStatus() == WAIT_FOR_SYNC && instance.isCreated()) {
            LOGGER.debug("Instance '{}' is reported as created on the cloud provider but not member of the cluster, setting its state to FAILED.",
                    instance.getInstanceId());
            instance.setInstanceStatus(InstanceStatus.FAILED);
            instanceMetaDataService.save(instance);
            eventService.fireCloudbreakEvent(stack.getId(), CREATE_FAILED.name(),
                    cloudbreakMessagesService.getMessage(Msg.STACK_SYNC_INSTANCE_FAILED.code(), Collections.singletonList(instance.getDiscoveryFQDN())));
        } else if (!instance.isRunning() && !instance.isDecommissioned() && !instance.isCreated() && !instance.isFailed()) {
            LOGGER.debug("Instance '{}' is reported as running on the cloud provider, updating metadata.", instance.getInstanceId());
            updateMetaDataToRunning(stack.getId(), stack.getCluster(), instance);
        }
    }

    private void syncDeletedInstance(Stack stack, Map<InstanceSyncState, Integer> instanceStateCounts, InstanceMetaData instance) {
        if (!instance.isTerminated()) {
            if (instance.getInstanceId() == null) {
                instanceStateCounts.put(InstanceSyncState.DELETED, instanceStateCounts.get(InstanceSyncState.DELETED) + 1);
                LOGGER.debug("Instance with private id '{}' don't have instanceId, setting its state to DELETED.",
                        instance.getPrivateId());
                instance.setInstanceStatus(InstanceStatus.TERMINATED);
                instanceMetaDataService.save(instance);
            } else {
                instanceStateCounts.put(InstanceSyncState.DELETED_ON_PROVIDER_SIDE, instanceStateCounts.get(InstanceSyncState.DELETED_ON_PROVIDER_SIDE) + 1);
                LOGGER.debug("Instance '{}' is reported as deleted on the cloud provider, setting its state to DELETED_ON_PROVIDER_SIDE.",
                        instance.getInstanceId());
                updateMetaDataToDeletedOnProviderSide(stack, instance);
            }
        }
    }

    private void handleSyncResult(Stack stack, Map<InstanceSyncState, Integer> instanceStateCounts, boolean stackStatusUpdateEnabled) {
        Set<InstanceMetaData> instances = instanceMetaDataService.findNotTerminatedForStack(stack.getId());
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
            updateStackStatusIfEnabled(stack.getId(), DetailedStackStatus.AVAILABLE, SYNC_STATUS_REASON, stackStatusUpdateEnabled);
            eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(),
                    cloudbreakMessagesService.getMessage(Msg.STACK_SYNC_INSTANCE_STATE_SYNCED.code()));
        } else if (instanceStateCounts.get(InstanceSyncState.STOPPED).equals(instances.size())) {
            updateStackStatusIfEnabled(stack.getId(), DetailedStackStatus.STOPPED, SYNC_STATUS_REASON, stackStatusUpdateEnabled);
            eventService.fireCloudbreakEvent(stack.getId(), STOPPED.name(),
                    cloudbreakMessagesService.getMessage(Msg.STACK_SYNC_INSTANCE_STATE_SYNCED.code()));
        } else {
            updateStackStatusIfEnabled(stack.getId(), DetailedStackStatus.DELETE_FAILED, SYNC_STATUS_REASON, stackStatusUpdateEnabled);
            eventService.fireCloudbreakEvent(stack.getId(), DELETE_FAILED.name(),
                    cloudbreakMessagesService.getMessage(Msg.STACK_SYNC_INSTANCE_STATE_SYNCED.code()));
        }
    }

    private void updateStackStatusIfEnabled(Long stackId, DetailedStackStatus status, String statusReason, boolean stackStatusUpdateEnabled) {
        if (stackStatusUpdateEnabled) {
            stackUpdater.updateStackStatus(stackId, status, statusReason);
        }
    }

    private Map<InstanceSyncState, Integer> initInstanceStateCounts() {
        Map<InstanceSyncState, Integer> instanceStates = new EnumMap<>(InstanceSyncState.class);
        instanceStates.put(InstanceSyncState.DELETED, 0);
        instanceStates.put(InstanceSyncState.DELETED_ON_PROVIDER_SIDE, 0);
        instanceStates.put(InstanceSyncState.STOPPED, 0);
        instanceStates.put(InstanceSyncState.RUNNING, 0);
        instanceStates.put(InstanceSyncState.IN_PROGRESS, 0);
        instanceStates.put(InstanceSyncState.UNKNOWN, 0);
        return instanceStates;
    }

    private void updateMetaDataToDeletedOnProviderSide(Stack stack, InstanceMetaData instanceMetaData) {
        instanceMetaData.setInstanceStatus(InstanceStatus.DELETED_ON_PROVIDER_SIDE);
        instanceMetaDataService.save(instanceMetaData);
        String name;
        name = instanceMetaData.getDiscoveryFQDN() == null ? instanceMetaData.getInstanceId() : String.format("%s (%s)", instanceMetaData.getInstanceId(),
                instanceMetaData.getDiscoveryFQDN());

        eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(),
                cloudbreakMessagesService.getMessage(Msg.STACK_SYNC_INSTANCE_DELETED_CBMETADATA.code(),
                        Collections.singletonList(name)));
    }

    private void updateMetaDataToRunning(Long stackId, Cluster cluster, InstanceMetaData instanceMetaData) {
        Optional<HostMetadata> hostMetadata = hostMetadataService.findHostInClusterByName(cluster.getId(), instanceMetaData.getDiscoveryFQDN());
        if (hostMetadata.isPresent()) {
            LOGGER.debug("Instance '{}' was found in the cluster metadata, setting it's state to REGISTERED.", instanceMetaData.getInstanceId());
            instanceMetaData.setInstanceStatus(InstanceStatus.REGISTERED);
        } else {
            LOGGER.debug("Instance '{}' was not found in the cluster metadata, setting it's state to UNREGISTERED.", instanceMetaData.getInstanceId());
            instanceMetaData.setInstanceStatus(InstanceStatus.UNREGISTERED);
        }
        instanceMetaDataService.save(instanceMetaData);
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

        private final String code;

        Msg(String msgCode) {
            code = msgCode;
        }

        public String code() {
            return code;
        }
    }
}