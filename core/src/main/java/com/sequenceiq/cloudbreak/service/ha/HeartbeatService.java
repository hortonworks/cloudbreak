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
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.api.client.util.Lists;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.core.flow2.Flow2Handler;
import com.sequenceiq.cloudbreak.core.flow2.FlowRegister;
import com.sequenceiq.cloudbreak.core.flow2.chain.FlowChains;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationFlowConfig;
import com.sequenceiq.cloudbreak.domain.CloudbreakNode;
import com.sequenceiq.cloudbreak.domain.FlowLog;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.CloudbreakNodeRepository;
import com.sequenceiq.cloudbreak.repository.FlowLogRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.Clock;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.service.Retry.ActionWentFail;

@Service
public class HeartbeatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeartbeatService.class);

    private static final List DELETE_STATUSES = Arrays.asList(Status.DELETE_IN_PROGRESS, Status.DELETE_COMPLETED, Status.DELETE_FAILED);

    @Value("${cb.ha.heartbeat.threshold:60000}")
    private Integer heartbeatThresholdRate;

    @Inject
    private CloudbreakNodeConfig cloudbreakNodeConfig;

    @Inject
    private CloudbreakNodeRepository cloudbreakNodeRepository;

    @Inject
    private FlowLogRepository flowLogRepository;

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
    private StackRepository stackRepository;

    @Inject
    private ReactorFlowManager reactorFlowManager;

    @Inject
    @Qualifier("DefaultRetryService")
    private Retry retryService;

    @Scheduled(cron = "${cb.ha.heartbeat.rate:0/30 * * * * *}")
    public void heartbeat() {
        if (cloudbreakNodeConfig.isNodeIdSpecified()) {
            String nodeId = cloudbreakNodeConfig.getId();
            try {
                retryService.testWith2SecDelayMax5Times(() -> {
                    try {
                        CloudbreakNode self = cloudbreakNodeRepository.findOne(nodeId);
                        if (self == null) {
                            self = new CloudbreakNode(nodeId);
                        }
                        self.setLastUpdated(clock.getCurrentTime());
                        cloudbreakNodeRepository.save(self);
                        return true;
                    } catch (RuntimeException e) {
                        LOGGER.error("Failed to update the heartbeat timestamp", e);
                        throw new ActionWentFail(e.getMessage());
                    }
                });
            } catch (ActionWentFail af) {
                LOGGER.error(String.format("Failed to update the heartbeat timestamp 5 times for node %s: %s", nodeId, af.getMessage()));
                cancelEveryFlowWithoutDbUpdate();
            }

            cancelInvalidFlows();
        }
    }

    @Scheduled(cron = "${cb.ha.flow.distribution.rate:0/35 * * * * *}")
    public void scheduledFlowDistribution() {
        if (cloudbreakNodeConfig.isNodeIdSpecified()) {
            List<CloudbreakNode> failedNodes = Collections.emptyList();
            try {
                failedNodes = distributeFlows();
            } catch (OptimisticLockingFailureException e) {
                LOGGER.error("Failed to distribute the flow logs across the active nodes, somebody might have already done it..", e);
            }

            try {
                cleanupNodes(failedNodes);
            } catch (RuntimeException e) {
                LOGGER.error("Failed to cleanup the nodes, somebody might have already done it..", e);
            }

            String nodeId = cloudbreakNodeConfig.getId();
            Set<String> allMyFlows = flowLogRepository.findAllByCloudbreakNodeId(nodeId).stream().map(FlowLog::getFlowId).distinct().collect(Collectors.toSet());
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

    @Transactional
    public List<CloudbreakNode> distributeFlows() {
        List<CloudbreakNode> cloudbreakNodes = Lists.newArrayList(cloudbreakNodeRepository.findAll());
        long currentTimeMillis = clock.getCurrentTime();
        List<CloudbreakNode> failedNodes = cloudbreakNodes.stream()
                .filter(node -> currentTimeMillis - node.getLastUpdated() > heartbeatThresholdRate).collect(Collectors.toList());
        List<CloudbreakNode> activeNodes = cloudbreakNodes.stream().filter(c -> !failedNodes.contains(c)).collect(Collectors.toList());
        LOGGER.info("Active CB nodes: ({})[{}], failed CB nodes: ({})[{}]", activeNodes.size(), activeNodes, failedNodes.size(), failedNodes);

        List<FlowLog> failedFlowLogs = failedNodes.stream()
                .map(node -> flowLogRepository.findAllByCloudbreakNodeId(node.getUuid()))
                .flatMap(Set::stream)
                .collect(Collectors.toList());

        if (!failedFlowLogs.isEmpty()) {
            LOGGER.info("The following flows will be distributed across the active nodes: {}", getFlowIds(failedFlowLogs));
            List<FlowLog> invalidFlows = getInvalidFlows(failedFlowLogs);
            List<FlowLog> updatedFlowLogs = new ArrayList<>(invalidFlows.size());
            invalidFlows.forEach(fl -> fl.setFinalized(true));
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
            flowLogRepository.save(updatedFlowLogs);
        }
        return failedNodes;
    }

    /**
     * Remove the node reference from the DB for those nodes that are failing and does not have any assigned flows.
     */
    @Transactional
    public void cleanupNodes(List<CloudbreakNode> failedNodes) {
        if (failedNodes != null && !failedNodes.isEmpty()) {
            LOGGER.info("Cleanup node candidates: {}", failedNodes);
            List<CloudbreakNode> cleanupNodes = failedNodes.stream()
                    .filter(node -> flowLogRepository.findAllByCloudbreakNodeId(node.getUuid()).isEmpty())
                    .collect(Collectors.toList());
            LOGGER.info("Cleanup nodes from the DB: {}", cleanupNodes);
            cloudbreakNodeRepository.delete(cleanupNodes);
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
            List<Object[]> stackStatuses = stackRepository.findStackStatuses(stackIds);
            for (Object[] ss : stackStatuses) {
                if (DELETE_STATUSES.contains(ss[1])) {
                    Long stackId = (Long) ss[0];
                    Set<String> runningFlowIds = flowLogRepository.findAllRunningNonTerminationFlowIdsByStackId(stackId);
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

    /**
     * Returns all the FlowLogs that have a termination flow running on any of the nodes for the same stack.
     * This is required as we don't want to distribute flows that will be terminated anyways.
     */
    private List<FlowLog> getInvalidFlows(List<FlowLog> flowLogs) {
        Set<Long> stackIds = flowLogs.stream().map(FlowLog::getStackId).distinct().collect(Collectors.toSet());
        if (!stackIds.isEmpty()) {
            Set<Long> deletingStackIds = stackRepository.findStackStatuses(stackIds).stream()
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

    private boolean hasRunningNonTerminationFlowOnThisNode(Set<String> runningFlowIds) {
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
        Stack stack = stackRepository.findOne(stackId);
        InMemoryStateStore.deleteStack(stackId);
        if (stack.getCluster() != null) {
            InMemoryStateStore.deleteCluster(stack.getCluster().getId());
        }
    }

    private List<String> getFlowIds(Collection<FlowLog> flowLogCollection) {
        return flowLogCollection.stream().map(FlowLog::getFlowId).distinct().collect(Collectors.toList());
    }

}
