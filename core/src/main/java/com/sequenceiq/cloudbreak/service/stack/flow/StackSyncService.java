package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.NODE_FAILURE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOPPED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UNREACHABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.WAIT_FOR_SYNC;
import static com.sequenceiq.cloudbreak.cloud.model.CloudInstance.INSTANCE_NAME;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_FAILED_NODES_REPORTED_CLUSTER_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_SYNC_INSTANCE_DELETED_BY_PROVIDER_CBMETADATA;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_SYNC_INSTANCE_DELETED_CBMETADATA;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_SYNC_INSTANCE_STATUS_RETRIEVAL_FAILED;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

@Service
public class StackSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackSyncService.class);

    private static final String SYNC_STATUS_REASON = "Synced instance states with the cloud provider.";

    private static final String CM_SERVER_NOT_RESPONDING = "Cloudera Manager server not responding.";

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private ImageService imageService;

    public void updateInstances(Stack stack, Iterable<InstanceMetaData> instanceMetaDataList, Collection<CloudVmInstanceStatus> instanceStatuses,
            SyncConfig syncConfig) {
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
                try {
                    syncInstanceStatusByState(stack, counts, metaData, state);
                } catch (TransactionRuntimeExecutionException e) {
                    LOGGER.error("Can't sync instance status by state!", e);
                }
            }
            handleSyncResult(stack, counts, syncConfig);
        } catch (CloudbreakImageNotFoundException | IllegalArgumentException ex) {
            LOGGER.info("Error during stack sync:", ex);
            throw new CloudbreakServiceException("Stack sync failed", ex);
        }
    }

    public void autoSync(Stack stack, Collection<InstanceMetaData> instances, List<CloudVmInstanceStatus> instanceStatuses,
            InstanceSyncState defaultState, SyncConfig syncConfig) {
        Map<String, InstanceSyncState> instanceSyncStates = instanceStatuses.stream()
                .collect(Collectors.toMap(i -> i.getCloudInstance().getInstanceId(), i -> InstanceSyncState.getInstanceSyncState(i.getStatus())));
        Map<InstanceSyncState, Integer> instanceStateCounts = initInstanceStateCounts();

        for (InstanceMetaData instance : instances) {
            try {
                InstanceSyncState instanceSyncState = calculateInstanceSyncState(defaultState, instanceSyncStates, instance);
                syncInstanceStatusByState(stack, instanceStateCounts, instance, instanceSyncState);
                if (!syncConfig.isCmServerRunning() && InstanceSyncState.RUNNING.equals(instanceSyncState) && instance.isPrimaryGateway()) {
                    instanceMetaDataService.updateInstanceStatus(instance, InstanceStatus.SERVICES_UNHEALTHY,
                            CM_SERVER_NOT_RESPONDING);
                }
            } catch (CloudConnectorException e) {
                LOGGER.warn(e.getMessage(), e);
                eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(),
                        STACK_SYNC_INSTANCE_STATUS_RETRIEVAL_FAILED,
                        Collections.singletonList(instance.getInstanceId()));
                incrementInstanceStateCount(instanceStateCounts, InstanceSyncState.UNKNOWN);
            }
        }
        handleSyncResult(stack, instanceStateCounts, syncConfig);
    }

    private InstanceSyncState calculateInstanceSyncState(InstanceSyncState defaultState, Map<String, InstanceSyncState> instanceSyncStates,
            InstanceMetaData instance) {
        InstanceSyncState instanceSyncState;
        if (instance.getInstanceId() == null) {
            instanceSyncState = InstanceSyncState.DELETED_ON_PROVIDER_SIDE;
        } else {
            instanceSyncState = instanceSyncStates.getOrDefault(instance.getInstanceId(), defaultState);
        }
        return instanceSyncState;
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
        if (InstanceSyncState.DELETED.equals(state)
                || InstanceSyncState.DELETED_ON_PROVIDER_SIDE.equals(state)
                || InstanceSyncState.DELETED_BY_PROVIDER.equals(state)) {
            syncDeletedInstance(stack, counts, metaData, state);
        } else if (InstanceSyncState.RUNNING.equals(state)) {
            syncRunningInstance(stack, counts, metaData);
        } else if (InstanceSyncState.STOPPED.equals(state)) {
            syncStoppedInstance(counts, metaData);
        } else {
            incrementInstanceStateCount(counts, state);
        }
    }

    private void syncStoppedInstance(Map<InstanceSyncState, Integer> instanceStateCounts, InstanceMetaData instance) {
        incrementInstanceStateCount(instanceStateCounts, InstanceSyncState.STOPPED);
        if (!instance.isTerminated() && instance.getInstanceStatus() != InstanceStatus.STOPPED) {
            LOGGER.debug("Instance '{}' is reported as stopped on the cloud provider, setting its state to STOPPED.", instance.getInstanceId());
            instanceMetaDataService.updateInstanceStatus(instance, InstanceStatus.STOPPED);
        }
    }

    private void syncRunningInstance(Stack stack, Map<InstanceSyncState, Integer> instanceStateCounts, InstanceMetaData instance) {
        incrementInstanceStateCount(instanceStateCounts, InstanceSyncState.RUNNING);
        if (stack.getStatus() == WAIT_FOR_SYNC && instance.isCreated()) {
            LOGGER.debug("Instance '{}' is reported as created on the cloud provider but not member of the cluster, setting its state to FAILED.",
                    instance.getInstanceId());
            instanceMetaDataService.updateInstanceStatus(instance, InstanceStatus.FAILED);
        } else if (!instance.isRunning()) {
            LOGGER.info("Instance '{}' is reported as running on the cloud provider, updating metadata.", instance.getInstanceId());
            updateMetaDataToRunning(instance);
        }
    }

    private void syncDeletedInstance(Stack stack, Map<InstanceSyncState, Integer> instanceStateCounts, InstanceMetaData instance,
            InstanceSyncState state) {
        if (!instance.isTerminated() || !instance.isDeletedOnProvider()) {
            if (instance.getInstanceId() == null) {
                if (instance.getDiscoveryFQDN() == null) {
                    incrementInstanceStateCount(instanceStateCounts, InstanceSyncState.DELETED);
                    LOGGER.debug("Instance with private id '{}' don't have instanceId and FQDN, setting its state to DELETED.",
                            instance.getPrivateId());
                    instanceMetaDataService.updateInstanceStatus(instance, InstanceStatus.TERMINATED);
                } else {
                    LOGGER.debug("Instance with private id '{}' don't have instanceId but it has FQDN", instance.getPrivateId());
                }
            } else {
                incrementInstanceStateCount(instanceStateCounts, state);
                LOGGER.debug("Instance '{}' is reported as deleted on the cloud provider, setting its state to {}.",
                        instance.getInstanceId(), state.name());
                eventService.fireCloudbreakEvent(stack.getId(), "RECOVERY",
                        CLUSTER_FAILED_NODES_REPORTED_CLUSTER_EVENT,
                        Collections.singletonList(instance.getDiscoveryFQDN()));
                if (InstanceSyncState.DELETED_BY_PROVIDER.equals(state)) {
                    updateMetaDataToDeletedByProvider(stack, instance);
                } else {
                    updateMetaDataToDeletedOnProviderSide(stack, instance);
                }
            }
        }
    }

    private void handleSyncResult(Stack stack, Map<InstanceSyncState, Integer> instanceStateCounts, SyncConfig syncConfig) {
        if (syncConfig.isStackStatusUpdateEnabled()) {
            Set<InstanceMetaData> instances = instanceMetaDataService.findNotTerminatedAndNotZombieForStackWithoutInstanceGroups(stack.getId());
            handleSyncResult(stack, instanceStateCounts, syncConfig, instances);
        }
    }

    void handleSyncResult(Stack stack, Map<InstanceSyncState, Integer> instanceStateCounts, SyncConfig syncConfig,
            Set<InstanceMetaData> instances) {
        Status status = stack.getStatus();
        if (instanceStateCounts.get(InstanceSyncState.RUNNING) > 0) {
            if (syncConfig.isCmServerRunning()) {
                if (status != AVAILABLE && status != NODE_FAILURE) {
                    updateStackStatus(stack.getId(), DetailedStackStatus.AVAILABLE, SYNC_STATUS_REASON);
                }
            } else if (status != UNREACHABLE) {
                updateStackStatus(stack.getId(), DetailedStackStatus.CLUSTER_MANAGER_NOT_RESPONDING, CM_SERVER_NOT_RESPONDING);
            }
        } else if (isAllStopped(instanceStateCounts, instances.size()) && status != STOPPED) {
            updateStackStatus(stack.getId(), DetailedStackStatus.STOPPED, SYNC_STATUS_REASON);
        } else if (isAllDeletedOnProvider(instanceStateCounts, instances.size()) && status != DELETE_FAILED) {
            updateStackStatus(stack.getId(), DetailedStackStatus.DELETED_ON_PROVIDER_SIDE, SYNC_STATUS_REASON);
        }
    }

    private boolean isAllDeletedOnProvider(Map<InstanceSyncState, Integer> instanceStateCounts, int numberOfInstances) {
        return instanceStateCounts.get(InstanceSyncState.DELETED_ON_PROVIDER_SIDE) + instanceStateCounts.get(InstanceSyncState.DELETED_BY_PROVIDER) > 0
                && numberOfInstances == 0;
    }

    private boolean isAllStopped(Map<InstanceSyncState, Integer> instanceStateCounts, int numberOfInstances) {
        return instanceStateCounts.get(InstanceSyncState.STOPPED).equals(numberOfInstances) && numberOfInstances > 0;
    }

    private Map<InstanceSyncState, Integer> initInstanceStateCounts() {
        Map<InstanceSyncState, Integer> instanceStates = new EnumMap<>(InstanceSyncState.class);
        for (InstanceSyncState instanceSyncState : InstanceSyncState.values()) {
            instanceStates.put(instanceSyncState, 0);
        }
        return instanceStates;
    }

    private void incrementInstanceStateCount(Map<InstanceSyncState, Integer> instanceStateCounts, InstanceSyncState state) {
        instanceStateCounts.put(state, instanceStateCounts.getOrDefault(state, 0) + 1);
    }

    private void updateStackStatus(Long stackId, DetailedStackStatus status, String statusReason) {
        stackUpdater.updateStackStatus(stackId, status, statusReason);
    }

    private void updateMetaDataToDeletedOnProviderSide(Stack stack, InstanceMetaData instanceMetaData) {
        instanceMetaDataService.updateInstanceStatus(instanceMetaData, InstanceStatus.DELETED_ON_PROVIDER_SIDE);
        eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(), STACK_SYNC_INSTANCE_DELETED_CBMETADATA,
                Collections.singletonList(getInstanceName(instanceMetaData)));
    }

    private void updateMetaDataToDeletedByProvider(Stack stack, InstanceMetaData instanceMetaData) {
        instanceMetaDataService.updateInstanceStatus(instanceMetaData, InstanceStatus.DELETED_BY_PROVIDER);
        eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(), STACK_SYNC_INSTANCE_DELETED_BY_PROVIDER_CBMETADATA,
                Collections.singletonList(getInstanceName(instanceMetaData)));
    }

    private void updateMetaDataToRunning(InstanceMetaData instanceMetaData) {
        LOGGER.info("Instance '{}' state to RUNNING.", instanceMetaData.getInstanceId());
        instanceMetaDataService.updateInstanceStatus(instanceMetaData, InstanceStatus.SERVICES_RUNNING, "Services running");
    }

    private String getInstanceName(InstanceMetaData instanceMetaData) {
        return instanceMetaData.getDiscoveryFQDN() == null
                ? instanceMetaData.getInstanceId()
                : String.format("%s (%s)", instanceMetaData.getInstanceId(), instanceMetaData.getDiscoveryFQDN());
    }
}