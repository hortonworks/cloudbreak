package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.domain.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.domain.Status.DELETE_FAILED;
import static com.sequenceiq.cloudbreak.domain.Status.STOPPED;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.connector.CloudConnectorException;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.InstanceStatus;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterConnector;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
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
    private AmbariClusterConnector ambariClusterConnector;
    @Resource
    private Map<CloudPlatform, MetadataSetup> metadataSetups;

    public void sync(Long stackId) {
        Stack stack = stackService.getById(stackId);
        Set<InstanceMetaData> instances = instanceMetaDataRepository.findAllInStack(stackId);
        Map<InstanceSyncState, Integer> instanceStateCounts = initInstanceStateCounts();
        for (InstanceMetaData instance : instances) {
            InstanceGroup instanceGroup = instance.getInstanceGroup();
            try {
                InstanceSyncState state = metadataSetups.get(stack.cloudPlatform()).getState(stack, instance.getInstanceId());
                if (InstanceSyncState.DELETED.equals(state)) {
                    instanceStateCounts.put(InstanceSyncState.DELETED, instanceStateCounts.get(InstanceSyncState.DELETED) + 1);
                    deleteHostFromCluster(stack, instance);
                    if (!instance.isTerminated()) {
                        LOGGER.info("Instance '{}' is reported as deleted on the cloud provider, setting its state to TERMINATED.", instance.getInstanceId());
                        updateMetaDataToTerminated(stackId, instance, instanceGroup);
                    }
                } else if (InstanceSyncState.RUNNING.equals(state)) {
                    instanceStateCounts.put(InstanceSyncState.RUNNING, instanceStateCounts.get(InstanceSyncState.RUNNING) + 1);
                    if (!instance.isRunning()) {
                        LOGGER.info("Instance '{}' is reported as running on the cloud provider, updating metadata.", instance.getInstanceId());
                        updateMetaDataToRunning(stackId, stack.getCluster(), instance, instanceGroup);
                    }
                } else if (InstanceSyncState.STOPPED.equals(state)) {
                    instanceStateCounts.put(InstanceSyncState.STOPPED, instanceStateCounts.get(InstanceSyncState.STOPPED) + 1);
                    if (!instance.isTerminated()) {
                        LOGGER.info("Instance '{}' is reported as stopped on the cloud provider, setting its state to STOPPED.", instance.getInstanceId());
                        updateMetaDataToTerminated(stackId, instance, instanceGroup);
                    }
                } else {
                    instanceStateCounts.put(InstanceSyncState.IN_PROGRESS, instanceStateCounts.get(InstanceSyncState.IN_PROGRESS) + 1);
                }
            } catch (CloudConnectorException e) {
                LOGGER.warn(e.getMessage(), e);
                eventService.fireCloudbreakEvent(stackId, AVAILABLE.name(),
                        String.format("Couldn't retrieve status of instance '%s' from cloud provider.", instance.getInstanceId()));
                instanceStateCounts.put(InstanceSyncState.UNKNOWN, instanceStateCounts.get(InstanceSyncState.UNKNOWN) + 1);
            }
        }

        handleSyncResult(stack, instanceStateCounts);
    }

    private void handleSyncResult(Stack stack, Map<InstanceSyncState, Integer> instanceStateCounts) {
        if (instanceStateCounts.get(InstanceSyncState.UNKNOWN) > 0) {
            eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(), "The state of one or more instances couldn't be determined. Try syncing later.");
        } else if (instanceStateCounts.get(InstanceSyncState.IN_PROGRESS) > 0) {
            eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(), "An operation on one or more instances is in progress. Try syncing later.");
        } else if (instanceStateCounts.get(InstanceSyncState.RUNNING) > 0 && instanceStateCounts.get(InstanceSyncState.STOPPED) > 0) {
            eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(),
                    "Some instances were stopped on the cloud provider. Restart or terminate them and try syncing later.");
        } else if (instanceStateCounts.get(InstanceSyncState.RUNNING) > 0) {
            stackUpdater.updateStackStatus(stack.getId(), AVAILABLE, SYNC_STATUS_REASON);
        } else if (instanceStateCounts.get(InstanceSyncState.STOPPED) > 0) {
            stackUpdater.updateStackStatus(stack.getId(), STOPPED, SYNC_STATUS_REASON);
        } else {
            stackUpdater.updateStackStatus(stack.getId(), DELETE_FAILED, SYNC_STATUS_REASON);
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
            HostMetadata hostMetadata = hostMetadataRepository.findHostsInClusterByName(stack.getCluster().getId(), instanceMetaData.getDiscoveryFQDN());
            if (hostMetadata != null) {
                ambariClusterConnector.deleteHostFromAmbari(stack, hostMetadata);
                hostMetadataRepository.delete(hostMetadata);
            }
        } catch (Exception e) {
            LOGGER.error("Host cannot be deleted from cluster: ", e);
            eventService.fireCloudbreakEvent(stack.getId(), DELETE_FAILED.name(),
                    String.format("Could not delete host '%s' from ambari.", instanceMetaData.getInstanceId()));
        }
    }

    private void updateMetaDataToTerminated(Long stackId, InstanceMetaData instanceMetaData, InstanceGroup instanceGroup) {
        instanceGroup.setNodeCount(instanceGroup.getNodeCount() - 1);
        instanceMetaData.setInstanceStatus(InstanceStatus.TERMINATED);
        instanceMetaDataRepository.save(instanceMetaData);
        instanceGroupRepository.save(instanceGroup);
        eventService.fireCloudbreakEvent(stackId, AVAILABLE.name(),
                String.format("Deleted instance '%s' from Cloudbreak metadata because it couldn't be found on the cloud provider.",
                        instanceMetaData.getInstanceId()));
    }

    private void updateMetaDataToRunning(Long stackId, Cluster cluster, InstanceMetaData instanceMetaData, InstanceGroup instanceGroup) {
        instanceGroup.setNodeCount(instanceGroup.getNodeCount() + 1);
        HostMetadata hostMetadata = hostMetadataRepository.findHostsInClusterByName(cluster.getId(), instanceMetaData.getDiscoveryFQDN());
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
                String.format("Updated metadata of instance '%s' to running because the cloud provider reported it as running.",
                        instanceMetaData.getInstanceId()));
    }


}
