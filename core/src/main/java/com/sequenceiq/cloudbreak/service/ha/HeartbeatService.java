package com.sequenceiq.cloudbreak.service.ha;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import com.sequenceiq.cloudbreak.domain.CloudbreakNode;
import com.sequenceiq.cloudbreak.domain.FlowLog;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.CloudbreakNodeRepository;
import com.sequenceiq.cloudbreak.repository.FlowLogRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.Clock;
import com.sequenceiq.cloudbreak.service.Retry;

@Service
public class HeartbeatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeartbeatService.class);

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
                    } catch (Exception e) {
                        LOGGER.error("Failed to update the heartbeat timestamp", e);
                        throw new Retry.ActionWentFail(e.getMessage());
                    }
                });
            } catch (Retry.ActionWentFail af) {
                LOGGER.error(String.format("Failed to update the heartbeat timestamp 5 times for node %s: %s", nodeId, af.getMessage()));
                cancelFlowsWithoutDbUpdate();
            }

            // query all stacks that have a running flow on this node
            Set<Long> stackIds = InMemoryStateStore.getAllStackId();
            if (!stackIds.isEmpty()) {
                List<Object[]> stackStatuses = stackRepository.findStackStatuses(stackIds);
                for (Object[] ss : stackStatuses) {
                    if (Status.DELETE_IN_PROGRESS.equals(ss[1]) || Status.DELETE_COMPLETED.equals(ss[1])) {
                        Long stackId = (Long) ss[0];
                        Set<String> runningFlowIds = flowLogRepository.findAllRunningNonTerminationFlowIdsByStackId(stackId);
                        if (hasRunningNonTerminationFlowOnThisNode(runningFlowIds)) {
                            cancelRunningFlow(stackId);
                        } else {
                            cleanupInMemoryStore(stackId);
                        }
                    }
                }
            }

        }
    }

    @Scheduled(cron = "${cb.ha.flow.distribution.rate:0/35 * * * * *}")
    public void scheduledFlowDistribution() {
        if (cloudbreakNodeConfig.isNodeIdSpecified()) {
            try {
                distributeFlows();
            } catch (OptimisticLockingFailureException e) {
                LOGGER.error("Failed to distribute the flow logs across the active nodes, somebody might have already done it..", e);
            }

            String nodeId = cloudbreakNodeConfig.getId();
            Set<String> allMyFlows = flowLogRepository.findAllByCloudbreakNodeId(nodeId).stream().map(FlowLog::getFlowId).distinct().collect(Collectors.toSet());
            Set<String> newFlows = allMyFlows.stream().filter(f -> runningFlows.get(f) == null).collect(Collectors.toSet());
            for (String flow : newFlows) {
                try {
                    flow2Handler.restartFlow(flow);
                } catch (Exception e) {
                    LOGGER.error(String.format("Failed to restart flow: %s", flow), e);
                }
            }
        }
    }

    @Transactional
    public void distributeFlows() {
        List<CloudbreakNode> cloudbreakNodes = Lists.newArrayList(cloudbreakNodeRepository.findAll());
        long currentTimeMillis = clock.getCurrentTime();
        List<CloudbreakNode> failedNodes = cloudbreakNodes.stream()
                .filter(cb -> currentTimeMillis - cb.getLastUpdated() > heartbeatThresholdRate).collect(Collectors.toList());
        List<CloudbreakNode> activeNodes = cloudbreakNodes.stream().filter(c -> !failedNodes.contains(c)).collect(Collectors.toList());
        LOGGER.info("Active CB nodes: ({})[{}], failed CB nodes: ({})[{}]", activeNodes.size(), activeNodes, failedNodes.size(), failedNodes);

        List<FlowLog> flowLogs = failedNodes.stream()
                .map(node -> flowLogRepository.findAllByCloudbreakNodeId(node.getUuid()))
                .flatMap(Set::stream)
                .collect(Collectors.toList());

        if (!flowLogs.isEmpty()) {
            List<String> flowIds = flowLogs.stream().map(FlowLog::getFlowId).distinct().collect(Collectors.toList());
            Map<CloudbreakNode, List<String>> flowDistribution = flowDistributor.distribute(flowIds, activeNodes);
            List<FlowLog> updatedFlowLogs = new ArrayList<>();
            for (CloudbreakNode node : flowDistribution.keySet()) {
                flowDistribution.get(node).forEach(flowId ->
                        flowLogs.stream().filter(flowLog -> flowLog.getFlowId().equalsIgnoreCase(flowId)).forEach(flowLog -> {
                            flowLog.setCloudbreakNodeId(node.getUuid());
                            updatedFlowLogs.add(flowLog);
                        }));
            }
            flowLogRepository.save(updatedFlowLogs);
        }
    }

    private boolean hasRunningNonTerminationFlowOnThisNode(Set<String> runningFlowIds) {
        return runningFlowIds.stream().anyMatch(id -> runningFlows.getFlowChainId(id) != null);
    }

    private void cancelFlowsWithoutDbUpdate() {
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

}
