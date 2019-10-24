package com.sequenceiq.cloudbreak.service.ha;

import java.util.ArrayList;
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

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryResourceStateStore;
import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.common.metrics.type.MetricType;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.ha.domain.Node;
import com.sequenceiq.cloudbreak.ha.service.NodeService;
import com.sequenceiq.cloudbreak.ha.service.FlowDistributor;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.service.flowlog.RestartFlowService;
import com.sequenceiq.flow.core.ApplicationFlowInformation;
import com.sequenceiq.flow.core.Flow2Handler;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.core.chain.FlowChains;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.StateStatus;
import com.sequenceiq.flow.ha.NodeConfig;

@Service
public class HeartbeatService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HeartbeatService.class);

    @Value("${cb.ha.heartbeat.threshold:60000}")
    private Integer heartbeatThresholdRate;

    @Inject
    private NodeConfig nodeConfig;

    @Inject
    private NodeService nodeService;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private RestartFlowService restartFlowService;

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
    private MetricService metricService;

    @Inject
    private HaApplication haApplication;

    @Inject
    @Qualifier("DefaultRetryService")
    private Retry retryService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private ApplicationFlowInformation applicationFlowInformation;

    @Scheduled(cron = "${cb.ha.heartbeat.rate:0/30 * * * * *}")
    public void heartbeat() {
        if (shouldRun()) {
            String nodeId = nodeConfig.getId();
            try {
                retryService.testWith2SecDelayMax5Times(() -> {
                    try {
                        Node self = nodeService.findById(nodeId).orElse(new Node(nodeId));
                        self.setLastUpdated(clock.getCurrentTimeMillis());
                        nodeService.save(self);
                        return Boolean.TRUE;
                    } catch (RuntimeException e) {
                        LOGGER.error("Failed to update the heartbeat timestamp", e);
                        metricService.incrementMetricCounter(MetricType.HEARTBEAT_UPDATE_FAILED);
                        throw new Retry.ActionFailedException(e.getMessage());
                    }
                });
            } catch (Retry.ActionFailedException af) {
                LOGGER.error("Failed to update the heartbeat timestamp 5 times for node {}: {}", nodeId, af.getMessage());
                cancelEveryFlowWithoutDbUpdate();
            }

            cancelInvalidFlows();
        }
    }

    @Scheduled(initialDelay = 35000L, fixedDelay = 30000L)
    public void scheduledFlowDistribution() {
        if (shouldRun()) {
            List<Node> failedNodes = new ArrayList<>();
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

            String nodeId = nodeConfig.getId();
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
        return nodeConfig.isNodeIdSpecified();
    }

    public List<Node> distributeFlows() throws TransactionExecutionException {
        List<Node> nodes = Lists.newArrayList(nodeService.findAll());
        long currentTimeMillis = clock.getCurrentTimeMillis();
        List<Node> failedNodes = nodes.stream()
                .filter(node -> currentTimeMillis - node.getLastUpdated() > heartbeatThresholdRate).collect(Collectors.toList());
        List<Node> activeNodes = nodes.stream().filter(c -> !failedNodes.contains(c)).collect(Collectors.toList());
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
            Map<Node, List<String>> flowDistribution = flowDistributor.distribute(getFlowIds(failedFlowLogs), activeNodes);
            for (Entry<Node, List<String>> entry : flowDistribution.entrySet()) {
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
    public void cleanupNodes(Collection<Node> failedNodes) throws TransactionExecutionException {
        if (failedNodes != null && !failedNodes.isEmpty()) {
            LOGGER.info("Cleanup node candidates: {}", failedNodes);
            List<Node> cleanupNodes = failedNodes.stream()
                    .filter(node -> flowLogService.findAllByCloudbreakNodeId(node.getUuid()).isEmpty())
                    .collect(Collectors.toList());
            LOGGER.info("Cleanup nodes from the DB: {}", cleanupNodes);
            transactionService.required(() -> {
                nodeService.deleteAll(cleanupNodes);
                return null;
            });
        }
    }

    /**
     * Query all resources that have a running flow on this node and if there is a termination flow on any of the nodes
     * cancel the non-terminating flows on this node for that resource
     */
    private void cancelInvalidFlows() {
        Set<Long> deletingResourceIds = haApplication.getAllDeletingResources();
        if (!deletingResourceIds.isEmpty()) {
            Set<Long> terminatingStacksByCurrentNode = findTerminatingStacksForCurrentNode();
            for (Long resourceId : deletingResourceIds) {
                if (isStackTerminationExecutedByAnotherNode(resourceId, terminatingStacksByCurrentNode)) {
                    Set<String> runningFlowIds = flowLogService.findAllRunningNonTerminationFlowIdsByStackId(resourceId);
                    if (haApplication.isRunningOnThisNode(runningFlowIds)) {
                        LOGGER.info("Found termination flow on a different node for stack: {}", resourceId);
                        cancelRunningFlow(resourceId);
                    } else {
                        cleanupInMemoryStore(resourceId);
                    }
                }
            }
        }
    }

    private boolean isStackTerminationExecutedByAnotherNode(Long stackId, Set<Long> terminatingStacksByCurrentNode) {
        return !terminatingStacksByCurrentNode.contains(stackId);
    }

    private Set<Long> findTerminatingStacksForCurrentNode() {
        return restartFlowService.findTerminatingStacksByCloudbreakNodeId(nodeConfig.getId());
    }

    /**
     * Returns all the FlowLogs that have a termination flow running on any of the nodes for the same stack.
     * This is required as we don't want to distribute flows that will be terminated anyways.
     */
    private List<FlowLog> getInvalidFlows(Collection<FlowLog> flowLogs) {
        Set<Long> resourceIds = flowLogs.stream().map(FlowLog::getResourceId).collect(Collectors.toSet());
        if (!resourceIds.isEmpty()) {
            Set<Long> deletingResourceIds = haApplication.getDeletingResources(resourceIds);
            if (!deletingResourceIds.isEmpty()) {
                return flowLogs.stream()
                        .filter(fl -> deletingResourceIds.contains(fl.getResourceId()))
                        .filter(fl -> applicationFlowInformation.getTerminationFlow().stream()
                                .map(Class::getName)
                                .noneMatch(terminationFlowClassName -> terminationFlowClassName.equals(fl.getFlowType().getName())))
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }

    private void cancelEveryFlowWithoutDbUpdate() {
        for (String resourceType : InMemoryResourceStateStore.getResourceTypes()) {
            for (Long resourceId : InMemoryResourceStateStore.getAllResourceId(resourceType)) {
                InMemoryResourceStateStore.putResource(resourceType, resourceId, PollGroup.CANCELLED);
            }
        }
        for (String id : runningFlows.getRunningFlowIds()) {
            String flowChainId = runningFlows.getFlowChainId(id);
            if (flowChainId != null) {
                flowChains.removeFullFlowChain(flowChainId);
            }
            runningFlows.remove(id);
        }
    }

    private void cancelRunningFlow(Long resourceId) {
        LOGGER.info("Cancel all running non-terminating flow for stack: {}", resourceId);
        haApplication.cancelRunningFlow(resourceId);
    }

    private void cleanupInMemoryStore(Long resourceId) {
        LOGGER.info("All running flows has been canceled for stack: {}. Remove id from memory store.", resourceId);
        haApplication.cleanupInMemoryStore(resourceId);
    }

    private List<String> getFlowIds(Collection<FlowLog> flowLogCollection) {
        return flowLogCollection.stream().map(FlowLog::getFlowId).distinct().collect(Collectors.toList());
    }

}
