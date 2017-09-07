package com.sequenceiq.cloudbreak.init;

import static com.sequenceiq.cloudbreak.api.model.Status.START_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.model.Status.STOP_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_REQUESTED;
import static com.sequenceiq.cloudbreak.api.model.Status.WAIT_FOR_SYNC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.model.InstanceStatus;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.controller.FlowsAlreadyRunningException;
import com.sequenceiq.cloudbreak.core.flow2.Flow2Handler;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.FlowLog;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.FlowLogRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.flowlog.FlowLogService;
import com.sequenceiq.cloudbreak.service.ha.CloudbreakNodeConfig;
import com.sequenceiq.cloudbreak.service.ha.HeartbeatService;
import com.sequenceiq.cloudbreak.service.usages.UsageService;

@Component
public class CloudbreakCleanupService implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakCleanupService.class);

    @Inject
    private CloudbreakNodeConfig cloudbreakNodeConfig;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private FlowLogRepository flowLogRepository;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private ReactorFlowManager flowManager;

    @Inject
    private UsageService usageService;

    @Inject
    private Flow2Handler flow2Handler;

    @Inject
    private HeartbeatService heartbeatService;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        heartbeatService.heartbeat();
        List<Long> stackIdsUnderOperation = restartOrUpdateUnassignedDisruptedFlows();
        stackIdsUnderOperation.addAll(restartMyAssignedDisruptedFlows());
        stackIdsUnderOperation.addAll(excludeStacksByFlowAssignment());
        usageService.fixUsages();
        List<Stack> stacksToSync = resetStackStatus(stackIdsUnderOperation);
        List<Cluster> clustersToSync = resetClusterStatus(stacksToSync, stackIdsUnderOperation);
        triggerSyncs(stacksToSync, clustersToSync);
    }

    private List<Stack> resetStackStatus(List<Long> excludeStackIds) {
        return stackRepository.findByStatuses(Arrays.asList(UPDATE_REQUESTED, UPDATE_IN_PROGRESS, WAIT_FOR_SYNC, START_IN_PROGRESS, STOP_IN_PROGRESS))
                .stream().filter(s -> !excludeStackIds.contains(s.getId()) || WAIT_FOR_SYNC.equals(s.getStatus()))
                .peek(s -> {
                    if (!WAIT_FOR_SYNC.equals(s.getStatus())) {
                        loggingStatusChange("Stack", s.getId(), s.getStatus(), WAIT_FOR_SYNC);
                        stackUpdater.updateStackStatus(s.getId(), DetailedStackStatus.WAIT_FOR_SYNC, s.getStatusReason());
                    }
                    cleanInstanceMetaData(instanceMetaDataRepository.findAllInStack(s.getId()));
                }).collect(Collectors.toList());
    }

    private void cleanInstanceMetaData(Set<InstanceMetaData> metadataSet) {
        for (InstanceMetaData metadata : metadataSet) {
            if (InstanceStatus.REQUESTED.equals(metadata.getInstanceStatus()) && metadata.getInstanceId() == null) {
                LOGGER.info("InstanceMetaData [privateId: '{}'] is deleted at CB start.", metadata.getPrivateId());
                instanceMetaDataRepository.delete(metadata);
            }
        }
    }

    private List<Cluster> resetClusterStatus(List<Stack> stacksToSync, List<Long> excludeStackIds) {
        return clusterRepository.findByStatuses(Arrays.asList(UPDATE_REQUESTED, UPDATE_IN_PROGRESS, WAIT_FOR_SYNC, START_IN_PROGRESS, STOP_IN_PROGRESS))
                .stream().filter(c -> !excludeStackIds.contains(c.getStack().getId()))
                .peek(c -> {
                    loggingStatusChange("Cluster", c.getId(), c.getStatus(), WAIT_FOR_SYNC);
                    c.setStatus(WAIT_FOR_SYNC);
                    clusterRepository.save(c);
                }).filter(c -> !stackToSyncContainsCluster(stacksToSync, c)).collect(Collectors.toList());
    }

    private boolean stackToSyncContainsCluster(List<Stack> stacksToSync, Cluster cluster) {
        Set<Long> stackIds = stacksToSync.stream().map(Stack::getId).collect(Collectors.toSet());
        return stackIds.contains(cluster.getStack().getId());
    }

    /**
     * Exclude all stacks that have active flows and assigned to Cb nodes.
     */
    private List<Long> excludeStacksByFlowAssignment() {
        List<Long> exclusion = new ArrayList<>();
        List<Object[]> allNonFinalized = flowLogRepository.findAllNonFinalized();
        allNonFinalized.stream().filter(o -> o[2] != null).forEach(o -> exclusion.add((Long) o[1]));
        return exclusion;
    }

    /**
     * If there are flow logs that do not have a Cloudbreak node id associated with it, we'll assigne a node to it (and let the node specific logic
     * to restart the flow). Otherwise we're going to restart the flow without association.
     */
    private List<Long> restartOrUpdateUnassignedDisruptedFlows() {
        List<Long> stackIds = new ArrayList<>();
        Set<FlowLog> unassignedFlowLogs = flowLogRepository.findAllUnassigned();
        if (!unassignedFlowLogs.isEmpty()) {
            if (cloudbreakNodeConfig.isNodeIdSpecified()) {
                try {
                    updateUnassignedFlows(unassignedFlowLogs, cloudbreakNodeConfig.getId());
                } catch (OptimisticLockingFailureException e) {
                    LOGGER.error("Failed to update the flow logs with node id. Maybe another node is already running them?", e);
                }
            } else {
                List<String> flowIds = unassignedFlowLogs.stream().map(FlowLog::getFlowId).distinct().collect(Collectors.toList());
                for (String flowId : flowIds) {
                    Long stackId = unassignedFlowLogs.stream().filter(f -> f.getFlowId().equalsIgnoreCase(flowId)).map(FlowLog::getStackId).findAny().get();
                    try {
                        flow2Handler.restartFlow(flowId);
                        stackIds.add(stackId);
                    } catch (RuntimeException e) {
                        LOGGER.error(String.format("Failed to restart flow %s on stack %s", flowId, stackId), e);
                        flowLogService.terminate(stackId, flowId);
                    }
                }
            }
        }
        return stackIds;
    }

    @Transactional
    private void updateUnassignedFlows(Set<FlowLog> flowLogs, String nodeId) {
        if (flowLogs != null && !flowLogs.isEmpty() && nodeId != null) {
            for (FlowLog flowLog : flowLogs) {
                flowLog.setCloudbreakNodeId(nodeId);
            }
            flowLogRepository.save(flowLogs);
        }
    }

    /**
     * It restarts all the disrupted flows that are assigned to this node.
     */
    private List<Long> restartMyAssignedDisruptedFlows() {
        List<Long> stackIds = new ArrayList<>();
        if (cloudbreakNodeConfig.isNodeIdSpecified()) {
            Set<FlowLog> myFlowLogs;
            try {
                myFlowLogs = getMyFlowLogs();
            } catch (ConcurrencyFailureException e) {
                LOGGER.error("Cannot restart my flows, they have been re-distributed to other nodes", e);
                return stackIds;
            }
            List<String> flowIds = myFlowLogs.stream().map(FlowLog::getFlowId).distinct().collect(Collectors.toList());
            for (String flowId : flowIds) {
                Long stackId = myFlowLogs.stream().filter(f -> f.getFlowId().equalsIgnoreCase(flowId)).map(FlowLog::getStackId).findAny().get();
                LOGGER.info("Restarting flow {}", flowId);
                try {
                    flow2Handler.restartFlow(flowId);
                    stackIds.add(stackId);
                } catch (RuntimeException e) {
                    LOGGER.error(String.format("Failed to restart flow %s on stack %s", flowId, stackId), e);
                    flowLogService.terminate(stackId, flowId);
                }
            }
        }
        return stackIds;
    }

    /**
     * Retrieves my assigned flow logs and updates the version lock to avoid concurrency issues.
     */
    @Transactional
    private Set<FlowLog> getMyFlowLogs() {
        Set<FlowLog> myFlowLogs = flowLogRepository.findAllByCloudbreakNodeId(cloudbreakNodeConfig.getId());
        myFlowLogs.forEach(fl -> fl.setCreated(fl.getCreated() + 1));
        flowLogRepository.save(myFlowLogs);
        return myFlowLogs;
    }

    private void loggingStatusChange(String type, Long id, Status status, Status deleteFailed) {
        LOGGER.info("{} {} status is updated from {} to {} at CB start.", type, id, status, deleteFailed);
    }

    private void triggerSyncs(List<Stack> stacksToSync, List<Cluster> clustersToSync) {
        try {
            for (Stack stack : stacksToSync) {
                LOGGER.info("Triggering full sync on stack [name: {}, id: {}].", stack.getName(), stack.getId());
                fireEvent(stack);
                flowManager.triggerFullSync(stack.getId());
            }

            for (Cluster cluster : clustersToSync) {
                Stack stack = cluster.getStack();
                LOGGER.info("Triggering sync on cluster [name: {}, id: {}].", cluster.getName(), cluster.getId());
                fireEvent(stack);
                flowManager.triggerClusterSync(stack.getId());
            }
        } catch (OptimisticLockingFailureException | FlowsAlreadyRunningException e) {
            LOGGER.error("Cannot trigger sync on stacks. Maybe another node is already syncing them?", e);
        }
    }

    private void fireEvent(Stack stack) {
        eventService.fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(),
                "Couldn't retrieve the cluster's status, starting to sync.");
    }
}

