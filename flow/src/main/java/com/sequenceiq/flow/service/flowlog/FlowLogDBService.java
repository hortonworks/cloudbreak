package com.sequenceiq.flow.service.flowlog;

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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.repository.ServiceFlowLogRepository;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.domain.FlowChainLog;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.StateStatus;
import com.sequenceiq.flow.ha.NodeConfig;
import com.sequenceiq.flow.repository.FlowLogRepository;

@Primary
@Service
public class FlowLogDBService implements FlowLogService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowLogDBService.class);

    @Inject
    private NodeConfig nodeConfig;

    @Inject
    private FlowLogRepository flowLogRepository;

    @Inject
    private FlowChainLogService flowChainLogService;

    @Inject
    @Qualifier("JsonWriterOptions")
    private Map<String, Object> writeOptions;

    @Inject
    private TransactionService transactionService;

    @Inject
    private ServiceFlowLogRepository serviceFlowLogRepository;

    public FlowLog save(FlowParameters flowParameters, String flowChanId, String key, Payload payload, Map<Object, Object> variables, Class<?> flowType,
            FlowState currentState) {
        String payloadAsString = getSerializedString(payload);
        String variablesJson = getSerializedString(variables);
        FlowLog flowLog = new FlowLog(payload.getResourceId(), flowParameters.getFlowId(), flowChanId, flowParameters.getFlowTriggerUserCrn(), key,
                payloadAsString, payload.getClass(), variablesJson, flowType, currentState.toString());
        flowLog.setCloudbreakNodeId(nodeConfig.getId());
        return flowLogRepository.save(flowLog);
    }

    public String getSerializedString(Object object) {
        String objectAsString;
        try {
            objectAsString = JsonWriter.objectToJson(object, writeOptions);
        } catch (Exception e) {
            LOGGER.error("Somehow can not serialize object to string, try another method..", e);
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
                objectAsString = objectMapper.writeValueAsString(object);
            } catch (JsonProcessingException jsonProcessingException) {
                LOGGER.error("Another json serializing method failed, sorry we have to give up.. :(", jsonProcessingException);
                objectAsString = "Can not serialize the object, please check logs";
            }
        }
        return objectAsString;
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

    private FlowLog finalize(Long stackId, String flowId, String state) throws TransactionExecutionException {
        return transactionService.required(() -> {
            flowLogRepository.finalizeByFlowId(flowId);
            getLastFlowLog(flowId).ifPresent(flowLog -> updateLastFlowLogStatus(flowLog, false));
            FlowLog flowLog = new FlowLog(stackId, flowId, state, Boolean.TRUE, StateStatus.SUCCESSFUL);
            flowLog.setCloudbreakNodeId(nodeConfig.getId());
            return flowLogRepository.save(flowLog);
        });
    }

    public void saveChain(String flowChainId, String parentFlowChainId, Queue<Selectable> chain, String flowTriggerUserCrn) {
        String chainJson = JsonWriter.objectToJson(chain);
        FlowChainLog chainLog = new FlowChainLog(flowChainId, parentFlowChainId, chainJson, flowTriggerUserCrn);
        flowChainLogService.save(chainLog);
    }

    public void updateLastFlowLogStatus(FlowLog lastFlowLog, boolean failureEvent) {
        StateStatus stateStatus = failureEvent ? StateStatus.FAILED : StateStatus.SUCCESSFUL;
        flowLogRepository.updateLastLogStatusInFlow(lastFlowLog.getId(), stateStatus);
    }

    public boolean isOtherFlowRunning(Long stackId) {
        Set<String> flowIds = flowLogRepository.findAllRunningNonTerminationFlowIdsByResourceId(stackId);
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
        return flowLogRepository.findAllRunningNonTerminationFlowIdsByResourceId(stackId);
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
        return flowLogRepository.findAllByResourceIdOrderByCreatedDesc(id);
    }

    public void purgeTerminatedStacksFlowLogs() throws TransactionService.TransactionExecutionException {
        transactionService.required(() -> {
            LOGGER.debug("Cleaning deleted stack's flowlog");
            int purgedTerminatedStackLogs = serviceFlowLogRepository.purgeTerminatedStackLogs();
            LOGGER.debug("Deleted flowlog count: {}", purgedTerminatedStackLogs);
            LOGGER.debug("Cleaning orphan flowchainlogs");
            int purgedOrphanFLowChainLogs = flowChainLogService.purgeOrphanFLowChainLogs();
            LOGGER.debug("Deleted flowchainlog count: {}", purgedOrphanFLowChainLogs);
            return null;
        });
    }

    public Set<Long> findTerminatingStacksByCloudbreakNodeId(String id) {
        return serviceFlowLogRepository.findTerminatingStacksByCloudbreakNodeId(id);
    }
}
