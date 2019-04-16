package com.sequenceiq.cloudbreak.service.ha;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.api.client.util.Lists;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.core.flow2.Flow2Handler;
import com.sequenceiq.cloudbreak.core.flow2.FlowLogService;
import com.sequenceiq.cloudbreak.core.flow2.FlowRegister;
import com.sequenceiq.cloudbreak.core.flow2.chain.FlowChains;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationFlowConfig;
import com.sequenceiq.cloudbreak.domain.CloudbreakNode;
import com.sequenceiq.cloudbreak.domain.FlowLog;
import com.sequenceiq.cloudbreak.domain.StateStatus;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.ha.CloudbreakNodeConfig;
import com.sequenceiq.cloudbreak.service.Clock;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.service.Retry.ActionWentFailException;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.metrics.CloudbreakMetricService;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;
import com.sequenceiq.cloudbreak.service.node.CloudbreakNodeService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class HeartbeatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeartbeatService.class);

    private static final List<Status> DELETE_STATUSES = Arrays.asList(Status.DELETE_IN_PROGRESS, Status.DELETE_COMPLETED, Status.DELETE_FAILED);

    @Value("${cb.ha.heartbeat.threshold:60000}")
    private Integer heartbeatThresholdRate;

    @Inject
    private CloudbreakNodeConfig cloudbreakNodeConfig;

    @Inject
    private CloudbreakNodeService cloudbreakNodeService;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private Flow2Handler flow2Handler;

    @Inject
    private Clock clock;

    @Inject
    private FlowDistributor flowDistributor;

    @Inject
    private FlowRegister runningFlows;

    @Inject
    private FlowChains flowChains;

    @Inject
    private StackService stackService;

    @Inject
    private ReactorFlowManager reactorFlowManager;

    @Inject
    private CloudbreakMetricService metricService;

    @Inject
    @Qualifier("DefaultRetryService")
    private Retry retryService;

    @Inject
    private TransactionService transactionService;

    @Scheduled(cron = "${cb.ha.heartbeat.rate:0/30 * * * * *}")
    public void heartbeat() {
        if (shouldRun()) {
            String nodeId = cloudbreakNodeConfig.getId();
            try {
                retryService.testWith2SecDelayMax5Times(() -> {
                    try {
                        CloudbreakNode self = cloudbreakNodeService.findById(nodeId).orElse(new CloudbreakNode(nodeId));
                        self.setLastUpdated(clock.getCurrentTimeMillis());
                        cloudbreakNodeService.save(self);
                        return Boolean.TRUE;
                    } catch (RuntimeException e) {
                        LOGGER.error("Failed to update the heartbeat timestamp", e);
                        metricService.incrementMetricCounter(MetricType.HEARTBEAT_UPDATE_FAILED);
                        throw new ActionWentFailException(e.getMessage());
                    }
                });
            } catch (ActionWentFailException af) {
                LOGGER.error("Failed to update the heartbeat timestamp 5 times for node {}: {}", nodeId, af.getMessage());
                cancelEveryFlowWithoutDbUpdate();
            }

            cancelInvalidFlows();
        }
    }

    @Scheduled(initialDelay = 35000L, fixedDelay = 30000L)
    public void scheduledFlowDistribution() {
        if (shouldRun()) {
            List<CloudbreakNode> failedNodes = new ArrayList<>();
            try {
                failedNodes.addAll(distributeFlows());
            } catch (TransactionExecutionException e) {
                LOGGER.error("Failed to distribute the flow logs across the active nodes, somebody might have already done it. Message: {}", e.getMessage());
            }
            try {
                cleanupNodes(failedNodes);
            } catch (TransactionExecutionException e) {
                LOGGER.error("Failed to cleanup the nodes, somebody might have already done it. Message: {}", e.getMessage());
            }

            String nodeId = cloudbreakNodeConfig.getId();
            Set<String> allMyFlows = flowLogService.findAllByCloudbreakNodeId(nodeId).stream()
                    .map(FlowLog::getFlowId).collect(Collectors.toSet());
            Set<String> newFlows = allMyFlows.stream().filter(f -> runningFlows.get(f) == null).collect(Collectors.toSet());
            for (String flow : newFlows) {
                try {
                    flow2Handler.restartFlow(flow);
                } catch (RuntimeException e) {
                    LOGGER.error(String.format("Failed to restart flow: %s", flow), e);
                }
            }
        }
    }

    private boolean shouldRun() {
        return cloudbreakNodeConfig.isNodeIdSpecified();
    }

    public List<CloudbreakNode> distributeFlows() throws TransactionExecutionException {
        List<CloudbreakNode> cloudbreakNodes = Lists.newArrayList(cloudbreakNodeService.findAll());
        long currentTimeMillis = clock.getCurrentTimeMillis();
        List<CloudbreakNode> failedNodes = cloudbreakNodes.stream()
                .filter(node -> currentTimeMillis - node.getLastUpdated() > heartbeatThresholdRate).collect(Collectors.toList());
        List<CloudbreakNode> activeNodes = cloudbreakNodes.stream().filter(c -> !failedNodes.contains(c)).collect(Collectors.toList());
        LOGGER.info("Active CB nodes: ({})[{}], failed CB nodes: ({})[{}]", activeNodes.size(), activeNodes, failedNodes.size(), failedNodes);

        List<FlowLog> failedFlowLogs = failedNodes.stream()
                .map(node -> flowLogService.findAllByCloudbreakNodeId(node.getUuid()))
                .flatMap(Set::stream)
                .collect(Collectors.toList());

        if (!failedFlowLogs.isEmpty()) {
            LOGGER.info("The following flows will be distributed across the active nodes: {}", getFlowIds(failedFlowLogs));
            List<FlowLog> invalidFlows = getInvalidFlows(failedFlowLogs);
            Collection<FlowLog> updatedFlowLogs = new ArrayList<>(invalidFlows.size());
            invalidFlows.forEach(fl -> {
                fl.setFinalized(true);
                fl.setStateStatus(StateStatus.SUCCESSFUL);
            });
            updatedFlowLogs.addAll(invalidFlows);
            failedFlowLogs.removeAll(invalidFlows);
            LOGGER.info("The following flows have been filtered out from distribution: {}", getFlowIds(invalidFlows));
            Map<CloudbreakNode, List<String>> flowDistribution = flowDistributor.distribute(getFlowIds(failedFlowLogs), activeNodes);
            for (Entry<CloudbreakNode, List<String>> entry : flowDistribution.entrySet()) {
                entry.getValue().forEach(flowId ->
                        failedFlowLogs.stream().filter(flowLog -> flowLog.getFlowId().equalsIgnoreCase(flowId)).forEach(flowLog -> {
                            flowLog.setCloudbreakNodeId(entry.getKey().getUuid());
                            updatedFlowLogs.add(flowLog);
                        }));
            }
            transactionService.required(() -> flowLogService.saveAll(updatedFlowLogs));
        }
        return failedNodes;
    }

    /**
     * Remove the node reference from the DB for those nodes that are failing and does not have any assigned flows.
     */
    public void cleanupNodes(Collection<CloudbreakNode> failedNodes) throws TransactionExecutionException {
        if (failedNodes != null && !failedNodes.isEmpty()) {
            LOGGER.info("Cleanup node candidates: {}", failedNodes);
            List<CloudbreakNode> cleanupNodes = failedNodes.stream()
                    .filter(node -> flowLogService.findAllByCloudbreakNodeId(node.getUuid()).isEmpty())
                    .collect(Collectors.toList());
            LOGGER.info("Cleanup nodes from the DB: {}", cleanupNodes);
            transactionService.required(() -> {
                cloudbreakNodeService.deleteAll(cleanupNodes);
                return null;
            });
        }
    }

    /**
     * Query all stacks that have a running flow on this node and if there is a termination flow on any of the nodes
     * cancel the non-terminating flows on this node for that stack
     */
    private void cancelInvalidFlows() {
        Set<Long> stackIds = InMemoryStateStore.getAllStackId();
        if (!stackIds.isEmpty()) {
            LOGGER.info("Check if there are termination flows for the following stack ids: {}", stackIds);
            List<Object[]> stackStatuses = stackService.getStatuses(stackIds);
            Set<Long> terminatingStacksByCurrentNode = findTerminatingStacksForCurrentNode();
            for (Object[] ss : stackStatuses) {
                if (DELETE_STATUSES.contains(ss[1])) {
                    Long stackId = (Long) ss[0];
                    if (isStackTerminationExecutedByAnotherNode(stackId, terminatingStacksByCurrentNode)) {
                        Set<String> runningFlowIds = flowLogService.findAllRunningNonTerminationFlowIdsByStackId(stackId);
                        if (hasRunningNonTerminationFlowOnThisNode(runningFlowIds)) {
                            LOGGER.info("Found termination flow on a different node for stack: {}", stackId);
                            cancelRunningFlow(stackId);
                        } else {
                            cleanupInMemoryStore(stackId);
                        }
                    }
                }
            }
        }
    }

    private boolean isStackTerminationExecutedByAnotherNode(Long stackId, Set<Long> terminatingStacksByCurrentNode) {
        return !terminatingStacksByCurrentNode.contains(stackId);
    }

    private Set<Long> findTerminatingStacksForCurrentNode() {
        return flowLogService.findTerminatingStacksByCloudbreakNodeId(cloudbreakNodeConfig.getId());
    }

    /**
     * Returns all the FlowLogs that have a termination flow running on any of the nodes for the same stack.
     * This is required as we don't want to distribute flows that will be terminated anyways.
     */
    private List<FlowLog> getInvalidFlows(Collection<FlowLog> flowLogs) {
        Set<Long> stackIds = flowLogs.stream().map(FlowLog::getStackId).collect(Collectors.toSet());
        if (!stackIds.isEmpty()) {
            Set<Long> deletingStackIds = stackService.getStatuses(stackIds).stream()
                    .filter(ss -> DELETE_STATUSES.contains(ss[1])).map(ss -> (Long) ss[0]).collect(Collectors.toSet());
            if (!deletingStackIds.isEmpty()) {
                return flowLogs.stream()
                        .filter(fl -> deletingStackIds.contains(fl.getStackId()))
                        .filter(fl -> !fl.getFlowType().equals(StackTerminationFlowConfig.class))
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }

    private boolean hasRunningNonTerminationFlowOnThisNode(Collection<String> runningFlowIds) {
        return runningFlowIds.stream().anyMatch(id -> runningFlows.getFlowChainId(id) != null);
    }

    private void cancelEveryFlowWithoutDbUpdate() {
        for (Long stackId : InMemoryStateStore.getAllStackId()) {
            InMemoryStateStore.putStack(stackId, PollGroup.CANCELLED);
        }
        for (Long clusterId : InMemoryStateStore.getAllClusterId()) {
            InMemoryStateStore.putStack(clusterId, PollGroup.CANCELLED);
        }
        for (String id : runningFlows.getRunningFlowIds()) {
            String flowChainId = runningFlows.getFlowChainId(id);
            if (flowChainId != null) {
                flowChains.removeFullFlowChain(flowChainId);
            }
            runningFlows.remove(id);
        }
    }

    private void cancelRunningFlow(Long stackId) {
        LOGGER.info("Cancel all running non-terminating flow for stack: {}", stackId);
        InMemoryStateStore.putStack(stackId, PollGroup.CANCELLED);
        reactorFlowManager.cancelRunningFlows(stackId);
    }

    private void cleanupInMemoryStore(Long stackId) {
        LOGGER.info("All running flows has been canceled for stack: {}. Remove id from memory store.", stackId);
        Stack stack = stackService.getByIdWithTransaction(stackId);
        InMemoryStateStore.deleteStack(stackId);
        if (stack.getCluster() != null) {
            InMemoryStateStore.deleteCluster(stack.getCluster().getId());
        }
    }

    private List<String> getFlowIds(Collection<FlowLog> flowLogCollection) {
        return flowLogCollection.stream().map(FlowLog::getFlowId).distinct().collect(Collectors.toList());
    }

}
