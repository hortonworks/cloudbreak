package com.sequenceiq.cloudbreak.service.flowlog;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.cedarsoftware.util.io.JsonWriter;
import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.FlowLogService;
import com.sequenceiq.cloudbreak.core.flow2.FlowState;
import com.sequenceiq.cloudbreak.domain.FlowChainLog;
import com.sequenceiq.cloudbreak.domain.FlowLog;
import com.sequenceiq.cloudbreak.domain.StateStatus;
import com.sequenceiq.cloudbreak.ha.CloudbreakNodeConfig;
import com.sequenceiq.cloudbreak.repository.FlowLogRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;

@Primary
@Service
public class FlowLogDBService implements FlowLogService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowLogDBService.class);

    @Inject
    private CloudbreakNodeConfig cloudbreakNodeConfig;

    @Inject
    private FlowLogRepository flowLogRepository;

    @Inject
    private FlowChainLogService flowChainLogService;

    @Inject
    @Qualifier("JsonWriterOptions")
    private Map<String, Object> writeOptions;

    @Inject
    private TransactionService transactionService;

    public FlowLog save(String flowId, String flowChanId, String key, Payload payload, Map<Object, Object> variables, Class<?> flowType,
            FlowState currentState) {
        String payloadJson = JsonWriter.objectToJson(payload, writeOptions);
        String variablesJson = JsonWriter.objectToJson(variables, writeOptions);
        FlowLog flowLog = new FlowLog(payload.getStackId(), flowId, flowChanId, key, payloadJson, payload.getClass(), variablesJson, flowType,
                currentState.toString());
        flowLog.setCloudbreakNodeId(cloudbreakNodeConfig.getId());
        return flowLogRepository.save(flowLog);
    }

    @Override
    public Iterable<FlowLog> saveAll(Iterable<FlowLog> flowLogs) {
        return flowLogRepository.saveAll(flowLogs);
    }

    public FlowLog close(Long stackId, String flowId) throws TransactionExecutionException {
        return finalize(stackId, flowId, "FINISHED");
    }

    public FlowLog cancel(Long stackId, String flowId) throws TransactionExecutionException {
        return finalize(stackId, flowId, "CANCELLED");
    }

    public FlowLog terminate(Long stackId, String flowId) throws TransactionExecutionException {
        return finalize(stackId, flowId, "TERMINATED");
    }

    public void purgeTerminatedStacksFlowLogs() throws TransactionExecutionException {
        transactionService.required(() -> {
            LOGGER.debug("Cleaning deleted stack's flowlog");
            int purgedTerminatedStackLogs = flowLogRepository.purgeTerminatedStackLogs();
            LOGGER.debug("Deleted flowlog count: {}", purgedTerminatedStackLogs);
            LOGGER.debug("Cleaning orphan flowchainlogs");
            int purgedOrphanFLowChainLogs = flowChainLogService.purgeOrphanFLowChainLogs();
            LOGGER.debug("Deleted flowchainlog count: {}", purgedOrphanFLowChainLogs);
            return null;
        });
    }

    private FlowLog finalize(Long stackId, String flowId, String state) throws TransactionExecutionException {
        return transactionService.required(() -> {
            flowLogRepository.finalizeByFlowId(flowId);
            getLastFlowLog(flowId).ifPresent(flowLog -> updateLastFlowLogStatus(flowLog, false));
            FlowLog flowLog = new FlowLog(stackId, flowId, state, Boolean.TRUE, StateStatus.SUCCESSFUL);
            flowLog.setCloudbreakNodeId(cloudbreakNodeConfig.getId());
            return flowLogRepository.save(flowLog);
        });
    }

    public void saveChain(String flowChainId, String parentFlowChainId, Queue<Selectable> chain) {
        String chainJson = JsonWriter.objectToJson(chain);
        FlowChainLog chainLog = new FlowChainLog(flowChainId, parentFlowChainId, chainJson);
        flowChainLogService.save(chainLog);
    }

    public void updateLastFlowLogStatus(FlowLog lastFlowLog, boolean failureEvent) {
        StateStatus stateStatus = failureEvent ? StateStatus.FAILED : StateStatus.SUCCESSFUL;
        flowLogRepository.updateLastLogStatusInFlow(lastFlowLog.getId(), stateStatus);
    }

    public boolean isOtherFlowRunning(Long stackId) {
        Set<String> flowIds = flowLogRepository.findAllRunningNonTerminationFlowIdsByStackId(stackId);
        return !flowIds.isEmpty();
    }

    public boolean repeatedFlowState(FlowLog lastFlowLog, String event) {
        return Optional.ofNullable(lastFlowLog).map(FlowLog::getNextEvent).map(flowLog -> flowLog.equalsIgnoreCase(event)).orElse(false);
    }

    public void updateLastFlowLogPayload(FlowLog lastFlowLog, Payload payload, Map<Object, Object> variables) {
        String payloadJson = JsonWriter.objectToJson(payload, writeOptions);
        String variablesJson = JsonWriter.objectToJson(variables, writeOptions);
        Optional.ofNullable(lastFlowLog)
                .ifPresent(flowLog -> {
                    flowLog.setPayload(payloadJson);
                    flowLog.setVariables(variablesJson);
                    flowLogRepository.save(flowLog);
                });
    }

    public Optional<FlowLog> getLastFlowLog(String flowId) {
        return flowLogRepository.findFirstByFlowIdOrderByCreatedDesc(flowId);
    }

    @Override
    public Set<String> findAllRunningNonTerminationFlowIdsByStackId(Long stackId) {
        return flowLogRepository.findAllRunningNonTerminationFlowIdsByStackId(stackId);
    }

    @Override
    public Optional<FlowLog> findFirstByFlowIdOrderByCreatedDesc(String flowId) {
        return flowLogRepository.findFirstByFlowIdOrderByCreatedDesc(flowId);
    }

    @Override
    public Optional<FlowChainLog> findFirstByFlowChainIdOrderByCreatedDesc(String flowChainId) {
        return flowChainLogService.findFirstByFlowChainIdOrderByCreatedDesc(flowChainId);
    }

    @Override
    public List<Object[]> findAllPending() {
        return flowLogRepository.findAllPending();
    }

    @Override
    public Set<FlowLog> findAllUnassigned() {
        return flowLogRepository.findAllUnassigned();
    }

    @Override
    public Set<FlowLog> findAllByCloudbreakNodeId(String cloudbreakNodeId) {
        return flowLogRepository.findAllByCloudbreakNodeId(cloudbreakNodeId);
    }

    public List<FlowLog> findAllByStackIdOrderByCreatedDesc(Long id) {
        return flowLogRepository.findAllByStackIdOrderByCreatedDesc(id);
    }

    @Override
    public Set<Long> findTerminatingStacksByCloudbreakNodeId(String id) {
        return flowLogRepository.findTerminatingStacksByCloudbreakNodeId(id);
    }

}
