package com.sequenceiq.flow.service.flowlog;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.google.common.base.Joiner;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.json.TypedJsonUtil;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.cloudbreak.util.Benchmark;
import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.core.ApplicationFlowInformation;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.ResourceIdProvider;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.FlowConfiguration;
import com.sequenceiq.flow.domain.ClassValue;
import com.sequenceiq.flow.domain.FlowChainLog;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.FlowLogIdWithTypeAndTimestamp;
import com.sequenceiq.flow.domain.FlowLogWithoutPayload;
import com.sequenceiq.flow.domain.StateStatus;
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
    private TransactionService transactionService;

    @Inject
    private ResourceIdProvider resourceIdProvider;

    @Inject
    private Clock clock;

    @Override
    public FlowLog save(FlowParameters flowParameters, String flowChainId, String key, Payload payload, Map<Object, Object> variables, Class<?> flowType,
            FlowState currentState) {
        String payloadJackson = JsonUtil.writeValueAsStringSilent(payload);
        String variablesJackson = TypedJsonUtil.writeValueAsStringSilent(variables);

        FlowLog flowLog = new FlowLog(payload.getResourceId(), flowParameters.getFlowId(), flowChainId, flowParameters.getFlowTriggerUserCrn(), key,
                payloadJackson, ClassValue.of(payload.getClass()), variablesJackson,
                ClassValue.of(flowType), currentState.toString());
        flowLog.setOperationType(StringUtils.isNotBlank(flowParameters.getFlowOperationType())
                ? OperationType.valueOf(flowParameters.getFlowOperationType())
                : OperationType.UNKNOWN);
        flowLog.setCloudbreakNodeId(nodeConfig.getId());
        if (payload.getException() != null) {
            flowLog.setReason(payload.getException().getMessage());
        }
        return flowLogRepository.save(flowLog);
    }

    @Override
    public Iterable<FlowLog> saveAll(Iterable<FlowLog> flowLogs) {
        return flowLogRepository.saveAll(flowLogs);
    }

    @Override
    public FlowLog finish(Long resourceId, String flowId, boolean failed, Map<Object, Object> contextParams, String reason)
            throws TransactionExecutionException {
        return finalize(resourceId, flowId, FlowConstants.FINISHED_STATE, failed, contextParams, reason);
    }

    @Override
    public FlowLog cancel(Long resourceId, String flowId) throws TransactionExecutionException {
        return finalize(resourceId, flowId, FlowConstants.CANCELLED_STATE, false, null, "Cancelled");
    }

    @Override
    public FlowLog terminate(Long resourceId, String flowId, String reason) throws TransactionExecutionException {
        LOGGER.info("Terminate flow [{}] for resource [{}] reason [{}]", flowId, resourceId, reason);
        FlowLog flowLog = finalize(resourceId, flowId, FlowConstants.TERMINATED_STATE, false, null, reason);
        applicationFlowInformation.handleFlowFail(flowLog);
        return flowLog;
    }

    private FlowLog finalize(Long resourceId, String flowId, String state, boolean failed, Map<Object, Object> contextParams, String reason)
            throws TransactionExecutionException {
        return transactionService.required(() -> {
            LOGGER.info("Finalize flow [{}] with state [{}] and failed [{}] for resource [{}]", flowId, state, failed, resourceId);
            Optional<FlowLog> lastFlowLogOpt = findFirstByFlowIdOrderByCreatedDesc(flowId);
            OperationType operationType = OperationType.UNKNOWN;
            if (lastFlowLogOpt.isPresent()) {
                FlowLog lastFlowLog = lastFlowLogOpt.get();
                LOGGER.info("Last FlowLog is available: {}", lastFlowLog);
                updateLastFlowLogStatus(lastFlowLog, failed, reason);
                operationType = lastFlowLog.getOperationType();
            }
            FlowLog flowLog = new FlowLog(resourceId, flowId, state, Boolean.TRUE, StateStatus.SUCCESSFUL, operationType);
            if (contextParams != null) {
                String variablesJackson = TypedJsonUtil.writeValueAsStringSilent(contextParams);
                flowLog.setVariablesJackson(variablesJackson);
            }
            lastFlowLogOpt.ifPresent(lastFlowLog -> {
                flowLog.setFlowType(lastFlowLog.getFlowType());
                flowLog.setFlowChainId(lastFlowLog.getFlowChainId());
                flowLog.setFlowTriggerUserCrn(lastFlowLog.getFlowTriggerUserCrn());
            });
            flowLog.setCloudbreakNodeId(nodeConfig.getId());
            flowLog.setReason(reason);
            flowLog.setEndTime(flowLog.getCreated());
            LOGGER.info("Persisting final FlowLog: {}", flowLog);
            FlowLog finalFlowLog = flowLogRepository.save(flowLog);
            finalizeAllFlowLogs(flowId);
            return finalFlowLog;
        });
    }

    private void finalizeAllFlowLogs(String flowId) {
        // We are using this method because FlowLog has @Version field, and it needs to be properly updated by JPA
        List<FlowLog> flowLogs = flowLogRepository.findAllByFlowIdOrderByCreatedDesc(flowId);
        for (FlowLog flowLog : flowLogs) {
            flowLog.setFinalized(Boolean.TRUE);
            if (StateStatus.PENDING.equals(flowLog.getStateStatus())) {
                flowLog.setStateStatus(StateStatus.SUCCESSFUL);
            }
        }
        flowLogRepository.saveAll(flowLogs);
    }

    @Override
    public void saveChain(String flowChainId, String parentFlowChainId, FlowTriggerEventQueue chain, String flowTriggerUserCrn) {
        LOGGER.debug("Saving the FlowChain info into FlowChainLog with chain: {}", chain);
        String chainType = chain.getFlowChainName();
        String chainJackson = TypedJsonUtil.writeValueAsStringSilent(chain.getQueue());
        String triggerEventJackson = null;
        if (chain.getTriggerEvent() != null) {
            triggerEventJackson = JsonUtil.writeValueAsStringSilent(chain.getTriggerEvent());
        }
        FlowChainLog chainLog = new FlowChainLog(chainType, flowChainId, parentFlowChainId, chainJackson, flowTriggerUserCrn, triggerEventJackson);
        flowChainLogService.save(chainLog);
    }

    @Override
    public void updateLastFlowLogStatus(FlowLog lastFlowLog, boolean failureEvent, String reason) {
        StateStatus stateStatus = failureEvent ? StateStatus.FAILED : StateStatus.SUCCESSFUL;
        lastFlowLog.setStateStatus(stateStatus);
        lastFlowLog.setEndTime(clock.getCurrentTimeMillis());
        lastFlowLog.setReason(reason);
        flowLogRepository.save(lastFlowLog);
    }

    @Override
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

    @Override
    public Set<FlowLogIdWithTypeAndTimestamp> findAllRunningFlowsByResourceId(Long resourceId) {
        return Benchmark.measure(() -> flowLogRepository.findAllRunningFlowLogByResourceId(resourceId), LOGGER,
                "Fetching all running flow for resource took {}ms");
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
        String payloadJackson = JsonUtil.writeValueAsStringSilent(payload);
        String variablesJackson = TypedJsonUtil.writeValueAsStringSilent(variables);
        Optional.ofNullable(lastFlowLog)
                .ifPresent(flowLog -> {
                    if (payload.getException() != null) {
                        flowLog.setReason(payload.getException().getMessage());
                    }
                    flowLog.setPayloadJackson(payloadJackson);
                    flowLog.setVariablesJackson(variablesJackson);
                    flowLogRepository.save(flowLog);
                });
    }

    @Override
    public Optional<FlowLogWithoutPayload> getLastFlowLog(String flowId) {
        return flowLogRepository.findByFlowIdOrderByCreatedDesc(flowId, Pageable.ofSize(1)).get().findFirst();
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

    @Override
    public Optional<FlowLog> getLastFlowLog(Long resourceId) {
        return flowLogRepository.findFirstByResourceIdOrderByCreatedDesc(resourceId);
    }

    @Override
    public Optional<FlowLog> getLastFlowLogWithEndTime(Long resourceId) {
        return flowLogRepository.findFirstByResourceIdAndEndTimeNotNullOrderByCreatedDesc(resourceId);
    }

    @Override
    public List<FlowLog> findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(Long id) {
        return flowLogRepository.findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(id);
    }

    @Override
    public int purgeFinalizedSuccessfulFlowLogs(int retentionPeriodHours) {
        long endTimeUpperBound = clock.nowMinus(Duration.ofHours(retentionPeriodHours)).toEpochMilli();
        return flowLogRepository.purgeFinalizedSuccessfulFlowLogs(endTimeUpperBound);
    }

    @Override
    public int purgeFinalizedFailedFlowLogs(int retentionPeriodHours) {
        long endTimeUpperBound = clock.nowMinus(Duration.ofHours(retentionPeriodHours)).toEpochMilli();
        return flowLogRepository.purgeFinalizedFailedFlowLogs(endTimeUpperBound);
    }

    @Override
    public List<FlowLog> findAllByFlowIdOrderByCreatedDesc(String flowId) {
        return flowLogRepository.findAllByFlowIdOrderByCreatedDesc(flowId);
    }

    public boolean isFlowConfigAlreadyRunning(Long id, Class<? extends FlowConfiguration<?>> flowConfigurationClass) {
        Optional<FlowLog> lastFlowLog = getLastFlowLog(id);
        boolean running = false;
        if (lastFlowLog.isPresent()) {
            FlowLog flowLog = lastFlowLog.get();
            running = flowLog.getFlowType() != null && flowLog.getFlowType().getClassValue().equals(flowConfigurationClass)
                    && flowLog.getStateStatus() == StateStatus.PENDING;
        }
        return running;
    }

    public Long getResourceIdByCrnOrName(String resource) {
        if (Crn.isCrn(resource)) {
            return resourceIdProvider.getResourceIdByResourceCrn(resource);
        }
        return resourceIdProvider.getResourceIdByResourceName(resource);
    }

    public List<Long> getResourceIdsByCrn(String resource) {
        if (Crn.isCrn(resource)) {
            return resourceIdProvider.getResourceIdsByResourceCrn(resource);
        } else {
            return Collections.emptyList();
        }
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

    public Page<FlowLog> getFlowLogsByFlowIdsCreatedDesc(Set<String> flowIds, Pageable page) {
        LOGGER.info("Getting flow logs by these flow ids: {}", Joiner.on(",").join(flowIds));
        if (!flowIds.isEmpty()) {
            return flowLogRepository.findAllByFlowIdsCreatedDesc(flowIds, page);
        } else {
            return new PageImpl<>(Collections.emptyList());
        }
    }

    public Boolean hasPendingFlowEvent(List<FlowLog> flowLogs) {
        LOGGER.debug("Checking if there is a pending flowEvent based on these flowLogs {}", Joiner.on(",")
                .join(flowLogs.stream().map(FlowLog::minimizedString).collect(Collectors.toList())));
        return flowLogs.stream().anyMatch(pendingFlowLogPredicate());
    }

    public <T extends AbstractFlowConfiguration> List<FlowLog> getLatestNotFinishedFlowLogsByCrnAndType(String resourceCrn, ClassValue classValue) {
        Long resourceId = getResourceIdByCrnOrName(resourceCrn);
        return flowLogRepository.findLastNotFinishedFlowLogsByTypeAndResourceId(resourceId, classValue);
    }

    public List<FlowLog> getLatestFlowLogsByCrnInFlowChain(String resourceCrn) {
        Long resourceId = getResourceIdByCrnOrName(resourceCrn);
        Optional<FlowLog> flowLogOpt = flowLogRepository.findFirstByResourceIdOrderByCreatedDesc(resourceId);
        if (flowLogOpt.isPresent()) {
            FlowLog flowLog = flowLogOpt.get();
            if (StringUtils.isNotBlank(flowLog.getFlowChainId())) {
                return flowLogRepository.findAllByFlowChainIdOrderByCreatedDesc(flowLog.getFlowChainId());
            } else {
                return flowLogRepository.findAllByFlowIdOrderByCreatedDesc(flowLog.getFlowId());
            }
        }
        return new ArrayList<>();
    }

    private Predicate<FlowLog> pendingFlowLogPredicate() {
        return flowLog -> flowLog.getStateStatus().equals(StateStatus.PENDING) || !flowLog.getFinalized();
    }

    public List<FlowLogWithoutPayload> getFlowLogsWithoutPayloadByFlowChainIdsCreatedDesc(Set<String> chainIds) {
        if (null != chainIds && !chainIds.isEmpty()) {
            LOGGER.info("Getting flow logs by these flow chain ids: {}", Joiner.on(",").join(chainIds));
            return flowLogRepository.findAllWithoutPayloadByChainIdsCreatedDesc(chainIds);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<FlowLogWithoutPayload> findAllWithoutPayloadByFlowIdOrderByCreatedDesc(String flowId) {
        return flowLogRepository.findAllWithoutPayloadByFlowIdOrderByCreatedDesc(flowId);
    }

    @Override
    public List<FlowLog> findAllFlowByFlowChainId(Set<String> chainIds) {
        List<FlowLog> flowDbLogs = flowLogRepository.findAllByFlowChainIdOrderByCreatedDesc(chainIds);
        return flowDbLogs.stream().collect(Collectors.groupingBy(FlowLog::getFlowId,
                        Collectors.reducing(BinaryOperator.maxBy(Comparator.comparing(FlowLog::getCreated)))))
                .values().stream().filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
    }

    public void closeFlowOnError(String flowId, String reason) {
        findFirstByFlowIdOrderByCreatedDesc(flowId).ifPresent(flowLog -> {
            try {
                applicationFlowInformation.handleFlowFail(flowLog);
            } catch (Exception e) {
                LOGGER.error("Exception occurred while handled {} flow failure. Message: {}", flowId, e.getMessage(), e);
            }
            updateLastFlowLogStatus(flowLog, true, reason);
            finalizeAllFlowLogs(flowId);
        });
    }
}
