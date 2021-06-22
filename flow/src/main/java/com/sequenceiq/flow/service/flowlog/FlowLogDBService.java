package com.sequenceiq.flow.service.flowlog;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.cedarsoftware.util.io.JsonWriter;
import com.google.common.base.Joiner;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.flow.core.ApplicationFlowInformation;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.ResourceIdProvider;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.domain.FlowChainLog;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.FlowLogIdWithTypeAndTimestamp;
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
    private ApplicationFlowInformation applicationFlowInformation;

    @Inject
    @Qualifier("JsonWriterOptions")
    private Map<String, Object> writeOptions;

    @Inject
    private TransactionService transactionService;

    @Inject
    private ResourceIdProvider resourceIdProvider;

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
            LOGGER.debug("Somehow can not serialize object to string, try another method..", e);
            objectAsString = JsonUtil.writeValueAsStringSilent(object);
        }
        return objectAsString;
    }

    @Override
    public Iterable<FlowLog> saveAll(Iterable<FlowLog> flowLogs) {
        return flowLogRepository.saveAll(flowLogs);
    }

    public FlowLog close(Long stackId, String flowId) throws TransactionExecutionException {
        return finalize(stackId, flowId, FlowConstants.FINISHED_STATE);
    }

    public FlowLog cancel(Long stackId, String flowId) throws TransactionExecutionException {
        return finalize(stackId, flowId, FlowConstants.CANCELLED_STATE);
    }

    public FlowLog terminate(Long stackId, String flowId) throws TransactionExecutionException {
        return finalize(stackId, flowId, FlowConstants.TERMINATED_STATE);
    }

    public void finalize(String flowId) {
        flowLogRepository.finalizeByFlowId(flowId);
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

    public void saveChain(String flowChainId, String parentFlowChainId, FlowTriggerEventQueue chain, String flowTriggerUserCrn) {
        String chainType = chain.getFlowChainName();
        String chainJson = JsonWriter.objectToJson(chain.getQueue());
        FlowChainLog chainLog = new FlowChainLog(chainType, flowChainId, parentFlowChainId, chainJson, flowTriggerUserCrn);
        flowChainLogService.save(chainLog);
    }

    public void updateLastFlowLogStatus(FlowLog lastFlowLog, boolean failureEvent) {
        StateStatus stateStatus = failureEvent ? StateStatus.FAILED : StateStatus.SUCCESSFUL;
        flowLogRepository.updateLastLogStatusInFlow(lastFlowLog.getId(), stateStatus);
    }

    public void cancelTooOldTerminationFlowForResource(Long resourceId, long olderThan) {
        Set<FlowLogIdWithTypeAndTimestamp> allRunningFlowIdsByResourceId = flowLogRepository.findAllRunningFlowLogByResourceId(resourceId);
        allRunningFlowIdsByResourceId.stream()
                .filter(flowLog -> applicationFlowInformation.getTerminationFlow().stream()
                        .map(Class::getName)
                        .anyMatch(terminationFlowClassName -> terminationFlowClassName.equals(flowLog.getFlowType().getName())))
                .filter(flowlog -> flowlog.getCreated() < olderThan)
                .findFirst().ifPresent(flowLog -> {
            try {
                LOGGER.info("Cancel flow [{}] for resource [{}] because it's too old", flowLog.getFlowId(), resourceId);
                cancel(resourceId, flowLog.getFlowId());
            } catch (TransactionExecutionException e) {
                LOGGER.error("Can't cancel termination flow: {}", flowLog.getFlowId(), e);
            }
        });
    }

    private Set<String> findAllRunningNonTerminationFlowIdsByResourceId(Long resourceId) {
        Set<FlowLogIdWithTypeAndTimestamp> allRunningFlowIdsByResourceId = flowLogRepository.findAllRunningFlowLogByResourceId(resourceId);
        return allRunningFlowIdsByResourceId.stream()
                .filter(flowLog -> applicationFlowInformation.getTerminationFlow().stream()
                        .map(Class::getName)
                        .noneMatch(terminationFlowClassName -> terminationFlowClassName.equals(flowLog.getFlowType().getName())))
                .map(FlowLogIdWithTypeAndTimestamp::getFlowId)
                .collect(Collectors.toSet());
    }

    public boolean isOtherNonTerminationFlowRunning(Long resourceId) {
        Set<String> flowIds = findAllRunningNonTerminationFlowIdsByResourceId(resourceId);
        return !flowIds.isEmpty();
    }

    private Set<String> findAllRunningFlowIdsByResourceId(Long resourceId) {
        Set<FlowLogIdWithTypeAndTimestamp> allRunningFlowIdsByResourceId = flowLogRepository.findAllRunningFlowLogByResourceId(resourceId);
        return allRunningFlowIdsByResourceId.stream()
                .map(FlowLogIdWithTypeAndTimestamp::getFlowId)
                .collect(Collectors.toSet());
    }

    public boolean isOtherFlowRunning(Long resourceId) {
        Set<String> flowIds = findAllRunningFlowIdsByResourceId(resourceId);
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
    public Set<String> findAllRunningNonTerminationFlowIdsByStackId(Long resourceId) {
        return findAllRunningNonTerminationFlowIdsByResourceId(resourceId);
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

    @Override
    public List<FlowLog> findAllForLastFlowIdByResourceIdOrderByCreatedDesc(Long id) {
        return flowLogRepository.findFirstByResourceIdOrderByCreatedDesc(id)
                .map(FlowLog::getFlowId)
                .map(flowLogRepository::findAllByFlowIdOrderByCreatedDesc)
                .orElse(List.of());
    }

    @Override
    public List<FlowLog> findAllByResourceIdOrderByCreatedDesc(Long id) {
        return flowLogRepository.findAllByResourceIdOrderByCreatedDesc(id);
    }

    public List<FlowLog> findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(Long id) {
        return flowLogRepository.findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(id);
    }

    @Override
    public int purgeFinalizedFlowLogs() {
        return flowLogRepository.purgeFinalizedFlowLogs();
    }

    public List<FlowLog> findAllByFlowIdOrderByCreatedDesc(String flowId) {
        return flowLogRepository.findAllByFlowIdOrderByCreatedDesc(flowId);
    }

    public Long getResourceIdByCrnOrName(String resource) {
        if (Crn.isCrn(resource)) {
            return resourceIdProvider.getResourceIdByResourceCrn(resource);
        }
        return resourceIdProvider.getResourceIdByResourceName(resource);
    }

    public FlowLog getLastFlowLogByResourceCrnOrName(String resource) {
        Long resourceId = getResourceIdByCrnOrName(resource);
        Iterator<FlowLog> iterator = findAllForLastFlowIdByResourceIdOrderByCreatedDesc(resourceId).iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        }
        throw new NotFoundException("Flow log for resource not found!");
    }

    public List<FlowLog> getFlowLogsByResourceCrnOrName(String resource) {
        Long resourceId = getResourceIdByCrnOrName(resource);
        return findAllForLastFlowIdByResourceIdOrderByCreatedDesc(resourceId);
    }

    public List<FlowLog> getAllFlowLogsByResourceCrnOrName(String resource) {
        Long resourceId = getResourceIdByCrnOrName(resource);
        return findAllByResourceIdOrderByCreatedDesc(resourceId);
    }

    public Set<String> getFlowIdsByChainIds(Set<String> flowChainIds) {
        LOGGER.info("Getting flow logs by these chain ids: {}", Joiner.on(",").join(flowChainIds));
        return flowLogRepository.findAllFlowIdsByChainIds(flowChainIds);
    }

    public List<FlowLog> getFlowLogsByFlowIdsCreatedDesc(Set<String> flowIds) {
        LOGGER.info("Getting flow logs by these flow ids: {}", Joiner.on(",").join(flowIds));
        if (!flowIds.isEmpty()) {
            return flowLogRepository.findAllByFlowIdsCreatedDesc(flowIds);
        } else {
            return Collections.emptyList();
        }
    }

    public Boolean hasPendingFlowEvent(List<FlowLog> flowLogs) {
        LOGGER.debug("Checking if there is a pending flowEvent based on these flowLogs {}", Joiner.on(",")
                .join(flowLogs.stream().map(flowLog -> flowLog.minimizedString()).collect(Collectors.toList())));
        return flowLogs.stream().anyMatch(pendingFlowLogPredicate());
    }

    public <T extends AbstractFlowConfiguration> List<FlowLog> getFlowLogsByCrnAndType(String resourceCrn, Class<T> clazz) {
        Long resourceId = getResourceIdByCrnOrName(resourceCrn);
        return flowLogRepository.findAllFlowByType(resourceId, clazz);
    }

    public <T extends AbstractFlowConfiguration> List<FlowLog> getLatestFlowLogsByCrnAndType(String resourceCrn, Class<T> clazz) {
        Long resourceId = getResourceIdByCrnOrName(resourceCrn);
        return flowLogRepository.findLastFlowLogsByTypeAndResourceId(resourceId, clazz);
    }

    public Predicate<FlowLog> pendingFlowLogPredicate() {
        return flowLog -> flowLog.getStateStatus().equals(StateStatus.PENDING) || !flowLog.getFinalized();
    }
}
