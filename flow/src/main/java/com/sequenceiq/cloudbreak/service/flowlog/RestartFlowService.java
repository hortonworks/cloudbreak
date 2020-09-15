package com.sequenceiq.cloudbreak.service.flowlog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.ha.service.ServiceFlowLogComponent;
import com.sequenceiq.flow.core.Flow2Handler;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.ha.NodeConfig;

@Service
public class RestartFlowService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestartFlowService.class);

    @Inject
    private NodeConfig nodeConfig;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private Flow2Handler flow2Handler;

    @Inject
    private ServiceFlowLogComponent serviceFlowLogComponent;

    @Inject
    private TransactionService transactionService;

    public List<Long> collectUnderOperationResources() throws Exception {
        List<Long> stackIdsUnderOperation = restartOrUpdateUnassignedDisruptedFlows();
        stackIdsUnderOperation.addAll(restartMyAssignedDisruptedFlows());
        stackIdsUnderOperation.addAll(excludeStacksByFlowAssignment());
        return stackIdsUnderOperation;
    }

    public Set<Long> findTerminatingStacksByCloudbreakNodeId(String id) {
        return serviceFlowLogComponent.findTerminatingResourcesByNodeId(id);
    }

    /**
     * If there are flow logs that do not have a Cloudbreak node id associated with it, we'll assigne a node to it (and let the node specific logic
     * to restart the flow). Otherwise we're going to restart the flow without association.
     */
    private List<Long> restartOrUpdateUnassignedDisruptedFlows() throws TransactionService.TransactionExecutionException {
        List<Long> stackIds = new ArrayList<>();
        Set<FlowLog> unassignedFlowLogs = flowLogService.findAllUnassigned();
        if (!unassignedFlowLogs.isEmpty()) {
            if (nodeConfig.isNodeIdSpecified()) {
                try {
                    updateUnassignedFlows(unassignedFlowLogs, nodeConfig.getId());
                } catch (TransactionService.TransactionExecutionException | OptimisticLockingFailureException e) {
                    LOGGER.error("Failed to update the flow logs with node id. Maybe another node is already running them?", e);
                }
            } else {
                List<String> flowIds = unassignedFlowLogs.stream().map(FlowLog::getFlowId).distinct().collect(Collectors.toList());
                for (String flowId : flowIds) {
                    Long stackId = unassignedFlowLogs.stream().filter(f -> f.getFlowId().equalsIgnoreCase(flowId)).map(FlowLog::getResourceId).findAny().get();
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

    /**
     * It restarts all the disrupted flows that are assigned to this node.
     */
    private Collection<Long> restartMyAssignedDisruptedFlows() throws TransactionService.TransactionExecutionException {
        Collection<Long> stackIds = new ArrayList<>();
        if (nodeConfig.isNodeIdSpecified()) {
            Set<FlowLog> myFlowLogs;
            try {
                myFlowLogs = getMyFlowLogs();
            } catch (TransactionService.TransactionExecutionException | ConcurrencyFailureException e) {
                LOGGER.error("Cannot restart my flows, they have been re-distributed to other nodes", e);
                return stackIds;
            }
            List<String> flowIds = myFlowLogs.stream().map(FlowLog::getFlowId).distinct().collect(Collectors.toList());
            for (String flowId : flowIds) {
                Long stackId = myFlowLogs.stream().filter(f -> f.getFlowId().equalsIgnoreCase(flowId)).map(FlowLog::getResourceId).findAny().get();
                LOGGER.debug("Restarting flow {}", flowId);
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
     * Exclude all stacks that have active flows and assigned to Cb nodes.
     */
    private Collection<Long> excludeStacksByFlowAssignment() {
        List<Long> exclusion = new ArrayList<>();
        List<Object[]> allPending = flowLogService.findAllPending();
        allPending.stream().filter(o -> o[2] != null).forEach(o -> exclusion.add((Long) o[1]));
        return exclusion;
    }

    private void updateUnassignedFlows(Collection<FlowLog> flowLogs, String nodeId) throws TransactionService.TransactionExecutionException {
        if (flowLogs != null && !flowLogs.isEmpty() && nodeId != null) {
            for (FlowLog flowLog : flowLogs) {
                flowLog.setCloudbreakNodeId(nodeId);
            }
            transactionService.required(() -> {
                flowLogService.saveAll(flowLogs);
                return null;
            });
        }
    }

    /**
     * Retrieves my assigned flow logs and updates the version lock to avoid concurrency issues.
     */
    private Set<FlowLog> getMyFlowLogs() throws TransactionService.TransactionExecutionException {
        Set<FlowLog> myFlowLogs = flowLogService.findAllByCloudbreakNodeId(nodeConfig.getId());
        myFlowLogs.forEach(fl -> fl.setCreated(fl.getCreated() + 1));
        transactionService.required(() -> flowLogService.saveAll(myFlowLogs));
        return myFlowLogs;
    }
}
