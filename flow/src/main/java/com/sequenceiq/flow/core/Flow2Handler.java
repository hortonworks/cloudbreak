package com.sequenceiq.flow.core;

import static com.sequenceiq.cloudbreak.service.flowlog.FlowLogUtil.deserializePayload;
import static com.sequenceiq.cloudbreak.service.flowlog.FlowLogUtil.deserializeVariables;
import static com.sequenceiq.flow.core.FlowConstants.FLOW_CANCEL;
import static com.sequenceiq.flow.core.FlowConstants.FLOW_CHAIN_ID;
import static com.sequenceiq.flow.core.FlowConstants.FLOW_CHAIN_TYPE;
import static com.sequenceiq.flow.core.FlowConstants.FLOW_FINAL;
import static com.sequenceiq.flow.core.FlowConstants.FLOW_ID;
import static com.sequenceiq.flow.core.FlowConstants.FLOW_OPERATION_TYPE;
import static com.sequenceiq.flow.core.FlowState.FlowStateConstants.INIT_STATE;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.common.event.IdempotentEvent;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.flowlog.FlowLogUtil;
import com.sequenceiq.cloudbreak.util.Benchmark;
import com.sequenceiq.cloudbreak.util.NullUtil;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.cleanup.InMemoryCleanup;
import com.sequenceiq.flow.core.cache.FlowStatCache;
import com.sequenceiq.flow.core.chain.FlowChainHandler;
import com.sequenceiq.flow.core.chain.FlowChains;
import com.sequenceiq.flow.core.config.FlowConfiguration;
import com.sequenceiq.flow.core.config.FlowFinalizerCallback;
import com.sequenceiq.flow.core.exception.FlowNotFoundException;
import com.sequenceiq.flow.core.exception.FlowNotTriggerableException;
import com.sequenceiq.flow.core.model.FlowAcceptResult;
import com.sequenceiq.flow.domain.FlowChainLog;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.FlowLogIdWithTypeAndTimestamp;
import com.sequenceiq.flow.service.flowlog.FlowChainLogService;

@Component
public class Flow2Handler implements Consumer<Event<? extends Payload>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Flow2Handler.class);

    @Inject
    private FlowLogService flowLogService;

    @Resource
    private List<FlowConfiguration<?>> flowConfigs;

    @Resource
    private Map<String, FlowConfiguration<?>> flowConfigurationMap;

    @Resource
    private Set<String> failHandledEvents;

    @Inject
    private FlowChains flowChains;

    @Inject
    private FlowChainHandler flowChainHandler;

    @Inject
    private FlowRegister runningFlows;

    @Inject
    private FlowStatCache flowStatCache;

    @Inject
    private TransactionService transactionService;

    @Inject
    private ApplicationFlowInformation applicationFlowInformation;

    @Inject
    private NodeConfig nodeConfig;

    @Inject
    private InMemoryCleanup inMemoryCleanup;

    @Inject
    private FlowChainLogService flowChainLogService;

    @Override
    public void accept(Event<? extends Payload> event) {
        doAccept(getFlowEventContext(event), getContextParams(event));
    }

    private void doAccept(FlowEventContext flowEventContext, Map<Object, Object> contextParams) {
        try {
            if (FLOW_CANCEL.equals(flowEventContext.getKey())) {
                cancelRunningFlows(flowEventContext.getResourceId());
            } else if (FLOW_FINAL.equals(flowEventContext.getKey())) {
                finalizeFlow(flowEventContext, contextParams, false, null);
            } else if (flowEventContext.getFlowId() == null) {
                AcceptResult result = handleNewFlowRequest(flowEventContext, contextParams);
                if (isAcceptablePayload(flowEventContext.getPayload())) {
                    ((Acceptable) flowEventContext.getPayload()).accepted().accept(result);
                }
            } else {
                handleFlowControlEvent(flowEventContext);
            }
        } catch (FlowNotTriggerableException e) {
            LOGGER.error("Failed to handle flow event.", e);
            if (isAcceptablePayload(flowEventContext.getPayload())) {
                ((Acceptable) flowEventContext.getPayload()).accepted().onError(e);
            } else {
                throw e;
            }
        } catch (CloudbreakServiceException e) {
            LOGGER.error("Failed to handle flow event.", e);
            throw e;
        } catch (Exception e) {
            LOGGER.error("Failed to handle flow event.", e);
            throw new CloudbreakServiceException(e);
        }
    }

    private void cancelRunningFlows(Long stackId) throws TransactionExecutionException {
        Set<String> flowIds = flowLogService.findAllRunningNonTerminationFlowIdsByStackId(stackId);
        LOGGER.debug("flow cancellation arrived: ids: {}", flowIds);
        for (String id : flowIds) {
            cancelFlow(stackId, id);
        }
    }

    public void cancelFlow(Long stackId, String flowId) throws TransactionExecutionException {
        LOGGER.debug("Cancel flow [{}] for stack [{}]", flowId, stackId);
        String flowChainId = runningFlows.getFlowChainId(flowId);
        if (flowChainId != null) {
            flowChains.removeFullFlowChain(flowChainId, false);
        }
        Flow flow = runningFlows.remove(flowId);
        flowStatCache.remove(flowId, false);
        if (flow != null) {
            flow.stop();
            flowLogService.cancel(stackId, flowId);
            createFinalizerCallback(flow).ifPresent(finalizerCallback -> finalizerCallback.onFinalize(stackId));
        }
    }

    private void finalizeFlow(FlowEventContext flowEventContext, Map<Object, Object> contextParams, boolean failed, String reason)
            throws TransactionExecutionException {
        String flowId = flowEventContext.getFlowId();
        LOGGER.debug("flow finalizing arrived: id: {}", flowId);
        flowLogService.finish(flowEventContext, contextParams, failed, reason);
        Flow flow = runningFlows.remove(flowId);
        if (flow != null) {
            Optional<FlowFinalizerCallback> finalizerCallback = createFinalizerCallback(flow);
            String flowChainId = flowEventContext.getFlowChainId();
            flowStatCache.remove(flowId, flowChainId == null && !flow.isFlowFailed());
            if (flowChainId != null) {
                if (flow.isFlowFailed()) {
                    flowChains.removeFullFlowChain(flowChainId, false);
                    finalizerCallback.ifPresent(callback -> callback.onFinalize(flowEventContext.getResourceId()));
                } else {
                    flowChains.triggerNextFlow(flowChainId, flowEventContext.getFlowTriggerUserCrn(), contextParams,
                            flowEventContext.getFlowOperationType(),
                            finalizerCallback.map(callback -> () -> callback.onFinalize(flowEventContext.getResourceId())));
                }
            } else {
                finalizerCallback.ifPresent(callback -> callback.onFinalize(flowEventContext.getResourceId()));
            }
        } else {
            LOGGER.warn("The flow is missing from the running flows, it has already been cancelled.");
        }
    }

    private Optional<FlowFinalizerCallback> createFinalizerCallback(Flow flow) {
        return flowConfigs.stream()
                .filter(flowConfiguration -> flow.getFlowConfigClass().equals(flowConfiguration.getClass()))
                .findFirst()
                .map(FlowConfiguration::getFinalizerCallBack);
    }

    private AcceptResult handleNewFlowRequest(FlowEventContext flowEventContext, Map<Object, Object> contextParams)
            throws TransactionExecutionException {
        String key = flowEventContext.getKey();
        Payload payload = flowEventContext.getPayload();
        String flowChainId = flowEventContext.getFlowChainId();
        LOGGER.debug("Flow trigger arrived: key: {}, payload: {}", key, payload);
        FlowConfiguration<?> flowConfig = getFlowConfiguration(key);
        FlowTriggerConditionResult flowTriggerConditionResult = flowConfig.getFlowTriggerCondition().isFlowTriggerable(payload);
        if (flowTriggerConditionResult.isFail()) {
            LOGGER.info("Trigger condition result: FAIL. Key: {}, flowChainId: {}, reason: {}", key, flowChainId, flowTriggerConditionResult.getErrorMessage());
            handleNotTriggerableFlow(flowEventContext, contextParams, false,
                    String.format("Trigger condition: fail, reason: %s", flowTriggerConditionResult.getErrorMessage()));
            throw new FlowNotTriggerableException(flowTriggerConditionResult.getErrorMessage());
        } else if (flowTriggerConditionResult.isSkip()) {
            LOGGER.info("Trigger condition result: SKIP. Key: {}, flowChainId: {}, reason: {}", key, flowChainId, flowTriggerConditionResult.getErrorMessage());
            handleNotTriggerableFlow(flowEventContext, contextParams, true,
                    String.format("Trigger condition: skip, reason: %s", flowTriggerConditionResult.getErrorMessage()));
            return getFlowAcceptResult(flowEventContext);
        } else {
            Set<FlowLogIdWithTypeAndTimestamp> flowLogItems = flowLogService.findAllRunningFlowsByResourceId(payload.getResourceId());
            if (hasRunningAndParallelNotAllowed(key, flowLogItems)) {
                return handleFlowConflict(flowEventContext, flowLogItems);
            } else {
                return createNewFlow(flowEventContext, contextParams, flowConfig);
            }
        }
    }

    private void handleNotTriggerableFlow(FlowEventContext flowEventContext, Map<Object, Object> contextParams, boolean success, String reason)
            throws TransactionExecutionException {
        transactionService.required(() -> {
            try {
                createNewFlowForClosure(flowEventContext, contextParams, success, reason);
                if (flowEventContext.getFlowChainId() != null) {
                    flowChains.removeLastTriggerEvent(flowEventContext.getFlowChainId(), flowEventContext.getFlowTriggerUserCrn());
                }
                finalizeFlow(flowEventContext, contextParams, !success, reason);
            } catch (TransactionExecutionException e) {
                throw new TransactionRuntimeExecutionException(e);
            }
        });
    }

    private void createNewFlowForClosure(FlowEventContext flowEventContext, Map<Object, Object> contextParams, boolean success, String reason) {
        String flowId = UUID.randomUUID().toString();
        LOGGER.info("Create new flow and immediately finalize it, because flow is not triggerable, key: {}, payload: {}, flowId: {} success: {}",
                flowEventContext.getKey(), flowEventContext.getPayload(), flowId, success);
        FlowConfiguration<?> flowConfig = getFlowConfiguration(flowEventContext.getKey());
        fillFlowEventContext(flowEventContext, flowId, flowConfig);
        flowLogService.save(flowEventContext, contextParams, flowConfig.getClass(), INIT_STATE);
        Flow flow = flowConfig.createFlow(flowId, flowEventContext.getFlowChainId(), flowEventContext.getResourceId(), flowEventContext.getFlowChainType());
        if (!success) {
            flow.setFlowFailed(new FlowNotTriggerableException(reason));
        }
        runningFlows.put(flow, flowEventContext.getFlowChainId());
    }

    private FlowConfiguration<?> getFlowConfiguration(String key) {
        FlowConfiguration<?> flowConfiguration = flowConfigurationMap.get(key);
        if (flowConfiguration == null) {
            LOGGER.error("Not found flow configuration for '{}' key.", key);
            throw new CloudbreakServiceException("Couldn't start process.");
        }
        return flowConfiguration;
    }

    private boolean hasRunningAndParallelNotAllowed(String key, Set<FlowLogIdWithTypeAndTimestamp> flowLogItems) {
        if (applicationFlowInformation.getAllowedParallelFlows().contains(key) || flowLogItems.isEmpty()) {
            if (!flowLogItems.isEmpty()) {
                LOGGER.info("Parallel flow request. Key: {}.", key);
            } else {
                LOGGER.info("Not found other running flow.");
            }
            return false;
        }
        return true;
    }

    private AcceptResult handleFlowConflict(FlowEventContext flowEventContext, Set<FlowLogIdWithTypeAndTimestamp> flowLogItems) {
        AcceptResult acceptResult = null;
        String flowChainId = flowEventContext.getFlowChainId();
        Optional<FlowLog> initFlowLog = flowLogService.findAllByFlowIdOrderByCreatedDesc(flowLogItems.iterator().next().getFlowId())
                .stream().min(Comparator.comparing(FlowLog::getCreated));
        if (initFlowLog.isPresent()) {
            LOGGER.info("Found previous init flow log: {}", initFlowLog.get());
            if (NullUtil.allNotNull(initFlowLog.get().getFlowChainId(), flowChainId)) {
                Optional<Pair<String, Payload>> previousTrigger = flowChains.getRootTriggerEvent(initFlowLog.get().getFlowChainId());
                Optional<Pair<String, Payload>> currentTrigger = flowChains.getRootTriggerEvent(flowChainId);
                if (previousTrigger.isPresent() && currentTrigger.isPresent()) {
                    if (isIdempotentTriggers(previousTrigger.get().getRight(), currentTrigger.get().getRight())) {
                        LOGGER.info("Idempotent flow chain trigger. Running {}, requested {}", previousTrigger, currentTrigger);
                        acceptResult = FlowAcceptResult.runningInFlowChain(previousTrigger.get().getLeft());
                    }
                }
            } else if (NullUtil.allNull(initFlowLog.get().getFlowChainId(), flowChainId)) {
                Payload previousTrigger = FlowLogUtil.tryDeserializePayload(initFlowLog.get());
                if (isIdempotentTriggers(previousTrigger, flowEventContext.getPayload())) {
                    LOGGER.info("Idempotent flow trigger. Running {}, requested {}", previousTrigger, flowEventContext.getPayload());
                    acceptResult = FlowAcceptResult.runningInFlow(initFlowLog.get().getFlowId());
                }
            }
        }
        if (acceptResult == null) {
            LOGGER.info("Flow operation not allowed, other flow is running. Resource ID {}, event {}",
                    flowEventContext.getResourceId(), flowEventContext.getKey());
            acceptResult = FlowAcceptResult.alreadyExistingFlow(flowLogItems);
        }
        flowChains.removeFullFlowChain(flowChainId, false);
        return acceptResult;
    }

    private boolean isIdempotentTriggers(Payload previousTrigger, Payload currentTrigger) {
        if (null == previousTrigger || null == currentTrigger) {
            return false;
        }
        if (!(previousTrigger instanceof IdempotentEvent) || !(currentTrigger instanceof IdempotentEvent)) {
            return false;
        }
        if (!previousTrigger.getClass().equals(currentTrigger.getClass())) {
            return false;
        }
        if (previousTrigger == currentTrigger) {
            return true;
        }
        return ((IdempotentEvent) currentTrigger).equalsEvent((IdempotentEvent) previousTrigger);
    }

    private FlowAcceptResult createNewFlow(FlowEventContext flowEventContext, Map<Object, Object> contextParams, FlowConfiguration<?> flowConfig)
            throws TransactionExecutionException {
        String flowId = UUID.randomUUID().toString();
        String flowChainId = flowEventContext.getFlowChainId();
        String key = flowEventContext.getKey();
        Payload payload = flowEventContext.getPayload();
        fillFlowEventContext(flowEventContext, flowId, flowConfig);
        Flow flow = flowConfig.createFlow(flowId, flowChainId, payload.getResourceId(), flowEventContext.getFlowChainType());
        LOGGER.debug("Creating new flow {}", flow.getFlowId());
        try {
            flow.initialize(contextParams);
            runningFlows.put(flow, flowChainId);
            Benchmark.measure(() -> flowStatCache.put(flowId, flowChainId, payload.getResourceId(),
                    flowConfig.getFlowOperationType().name(), flow.getFlowConfigClass(), false), LOGGER, "Creating flow stat took {}ms");
            transactionService.required(() -> {
                flowLogService.save(flowEventContext, contextParams, flowConfig.getClass(), flow.getCurrentState());
                if (flowChainId != null) {
                    LOGGER.debug("Updating FlowChain [{}] when creating new flow", flowChainId);
                    flowChains.saveAllUnsavedFlowChains(flowChainId, flowEventContext.getFlowTriggerUserCrn());
                    flowChains.removeLastTriggerEvent(flowChainId, flowEventContext.getFlowTriggerUserCrn());
                }
            });
            logFlowId(flowId);
            FlowAcceptResult flowAcceptResult = getFlowAcceptResult(flowEventContext);
            if (isAcceptablePayload(payload)) {
                LOGGER.info("Accepting flow {}", flowAcceptResult);
                ((Acceptable) payload).accepted().accept(flowAcceptResult);
            }
            boolean accepted = flow.sendEvent(flowEventContext);
            if (!accepted) {
                LOGGER.info("Couldn't trigger {} flow because state machine not accepted {} event", flowConfig.getDisplayName(), key);
                throw new FlowNotTriggerableException(String.format("Couldn't trigger '%s' flow.", flowConfig.getDisplayName()));
            }
            LOGGER.info("Flow started '{}'. Start event: {}", flowConfig.getDisplayName(), payload);
            return flowAcceptResult;
        } catch (Exception e) {
            LOGGER.error("Can't save flow: {}", flowId, e);
            runningFlows.remove(flowId);
            flowStatCache.remove(flowId, false);
            if (flowChainId != null) {
                flowChains.removeFullFlowChain(flowChainId, false);
            }
            flow.stop();
            throw e;
        }
    }

    private void fillFlowEventContext(FlowEventContext flowEventContext, String flowId, FlowConfiguration<?> flowConfig) {
        flowEventContext.setFlowId(flowId);
        if (flowEventContext.getFlowChainId() == null) {
            flowEventContext.setFlowOperationType(flowConfig.getFlowOperationType().name());
        }
    }

    private FlowAcceptResult getFlowAcceptResult(FlowEventContext flowEventContext) {
        FlowAcceptResult acceptResult;
        if (flowEventContext.getFlowChainId() != null) {
            acceptResult = FlowAcceptResult.runningInFlowChain(flowEventContext.getFlowChainId());
        } else {
            acceptResult = FlowAcceptResult.runningInFlow(flowEventContext.getFlowId());
        }
        return acceptResult;
    }

    private void handleFlowControlEvent(FlowEventContext flowEventContext) throws TransactionExecutionException {
        String flowId = flowEventContext.getFlowId();
        String key =  flowEventContext.getKey();
        String flowChainId =  flowEventContext.getFlowChainId();
        Payload payload = flowEventContext.getPayload();
        LOGGER.debug("flow control event arrived: key: {}, flowid: {}, usercrn: {}, payload: {}",
                key, flowId, flowEventContext.getFlowTriggerUserCrn(), payload);
        Flow flow = runningFlows.get(flowId);
        if (flow != null) {
            MutableBoolean flowCancelled = new MutableBoolean(false);
            try {
                updateFlowLogStatusInTransaction(flowEventContext, flow, flowCancelled);
            } catch (TransactionExecutionException e) {
                LOGGER.error("Can't update flow status: {}", flowId);
                throw e;
            }
            if (!flowCancelled.booleanValue()) {
                LOGGER.debug("Send event: key: {}, flowid: {}, usercrn: {}, payload: {}", key, flowId, flowEventContext.getFlowTriggerUserCrn(), payload);
                boolean accepted = flow.sendEvent(flowEventContext);
                if (!accepted) {
                    LOGGER.info("Couldn't trigger {} flow because state machine not accepted {} event", flow.getFlowConfigClass(), key);
                    throw new CloudbreakServiceException(String.format("Couldn't trigger state machine with %s event in flow %s.", key, flowId));
                }
                if (isAcceptablePayload(payload)) {
                    FlowAcceptResult flowAcceptResult = FlowAcceptResult.runningInFlow(flowId);
                    LOGGER.info("Accepting flow in flow control {}", flowAcceptResult);
                    ((Acceptable) payload).accepted().accept(flowAcceptResult);
                }
            }
        } else {
            LOGGER.debug("Cancelled flow finished running. Resource ID {}, flow ID {}, event {}", payload.getResourceId(), flowId, key);
        }
    }

    private void updateFlowLogStatusInTransaction(FlowEventContext flowEventContext, Flow flow, MutableBoolean flowCancelled)
            throws TransactionExecutionException {
        transactionService.required(() -> {
            Optional<FlowLog> lastFlowLog = flowLogService.findFirstByFlowIdOrderByCreatedDesc(flow.getFlowId());
            if (lastFlowLog.isPresent()) {
                String nodeId = nodeConfig.getId();
                FlowLog flowLog = lastFlowLog.get();
                if (flowLog.getFinalized() || flowLog.getCloudbreakNodeId() == null || flowLog.getCloudbreakNodeId().equals(nodeId)) {
                    updateFlowLogStatus(flowEventContext, flow, flowLog);
                } else {
                    LOGGER.info("Flow {} was handled by another node {}, current node ID is {}, abandoning.",
                            flow.getFlowId(), flowLog.getCloudbreakNodeId(), nodeId);
                    inMemoryCleanup.cancelFlowWithoutDbUpdate(flow.getFlowId());
                    flowCancelled.setTrue();
                }
            } else {
                LOGGER.debug("Cannot find LastFlowLog with flowId: {}", flow.getFlowId());
            }
        });
    }

    private void updateFlowLogStatus(FlowEventContext flowEventContext, Flow flow, FlowLog lastFlowLog) {
        Payload payload = flowEventContext.getPayload();
        if (flowLogService.repeatedFlowState(lastFlowLog, flowEventContext.getKey())) {
            LOGGER.debug("Repeated flow state: {}, key: {}", lastFlowLog, flowEventContext.getKey());
            flowLogService.updateLastFlowLogPayload(lastFlowLog, payload, flow.getVariables());
        } else {
            boolean failureEvent = failHandledEvents.contains(flowEventContext.getKey());
            LOGGER.debug("New flow state: {}, key: {}, failure event: {}", lastFlowLog, flowEventContext.getKey(), failureEvent);
            String reason = payload.getException() != null ? payload.getException().getMessage() : null;
            flowLogService.updateLastFlowLogStatus(lastFlowLog, failureEvent, reason);
            flowLogService.save(flowEventContext, flow.getVariables(), flow.getFlowConfigClass(), flow.getCurrentState());
        }
    }

    private boolean isAcceptablePayload(Payload payload) {
        return payload instanceof Acceptable
                && ((Acceptable) payload).accepted() != null
                && !((Acceptable) payload).accepted().isComplete();
    }

    public FlowIdentifier retryLastFailedFlow(Long resourceId, java.util.function.Consumer<FlowLog> beforeRestart) {
        List<FlowLog> flowLogs = findAllForLastFlowIdChecked(resourceId);
        return FlowLogUtil.getMostRecentFailedLog(flowLogs)
                .map(log -> FlowLogUtil.getLastSuccessfulStateLog(log.getCurrentState(), flowLogs))
                .map(lastSuccessfulStateLog -> {
                    LOGGER.info("Trying to restart flow from: {}", lastSuccessfulStateLog);
                    beforeRestart.accept(lastSuccessfulStateLog);
                    restartFlow(lastSuccessfulStateLog);
                    LOGGER.info("Restarted flow from: {}", lastSuccessfulStateLog);
                    if (lastSuccessfulStateLog.getFlowChainId() != null) {
                        return new FlowIdentifier(FlowType.FLOW_CHAIN, lastSuccessfulStateLog.getFlowChainId());
                    } else {
                        return new FlowIdentifier(FlowType.FLOW, lastSuccessfulStateLog.getFlowId());
                    }
                })
                .orElseThrow(() -> new BadRequestException("Retry cannot be performed, because the last action was successful."));
    }

    /**
     * Retry the failed flow completely
     *
     * @param resourceId Datalake ID
     * @return Identifier of flow or a flow chain
     */
    public FlowIdentifier retryLastFailedFlowFromStart(Long resourceId) {
        FlowLog firstSuccessfulStateLog = getFirstRetryableStateLogfromLatestFlow(resourceId);
        LOGGER.info("Trying to restart flow {}", firstSuccessfulStateLog.getFlowType().getName());
        restartFlow(firstSuccessfulStateLog);
        LOGGER.info("Restarted flow : {}", firstSuccessfulStateLog.getFlowType().getName());
        if (firstSuccessfulStateLog.getFlowChainId() != null) {
            return new FlowIdentifier(FlowType.FLOW_CHAIN, firstSuccessfulStateLog.getFlowChainId());
        } else {
            return new FlowIdentifier(FlowType.FLOW, firstSuccessfulStateLog.getFlowId());
        }
    }

    public void restartFlow(String flowId) {
        FlowLog flowLog = flowLogService.findFirstByFlowIdOrderByCreatedDesc(flowId).orElseThrow(() -> new FlowNotFoundException(flowId));
        restartFlow(flowLog);
    }

    public void restartFlow(FlowLog flowLog) {
        try {
            if (notSupportedFlowType(flowLog)) {
                terminateFlow(flowLog, "Terminate flow, not supported flow type");
            } else if (isRestartableFlow(flowLog)) {
                continueFlow(flowLog);
            } else if (isRestartableFlowChain(flowLog)) {
                continueFlowChain(flowLog);
            }
        } catch (Exception e) {
            LOGGER.error("Flow could not be restarted with id: '{}', flow chain id: '{}' and flow type: '{}'",
                    flowLog.getFlowId(),
                    flowLog.getFlowChainId(),
                    flowLog.getFlowType().getClassValue().getSimpleName(),
                    e);
            terminateFlow(flowLog, String.format("Flow restart failed, reason: %s", e.getMessage()));
        }
    }

    private boolean notSupportedFlowType(FlowLog flowLog) {
        if (flowLog.getFlowType() != null && !flowLog.getFlowType().isOnClassPath()) {
            LOGGER.error("Flow type '{}' is not on classpath.", flowLog.getFlowType().getName());
            return true;
        }

        if (flowLog.getPayloadType() != null && !flowLog.getPayloadType().isOnClassPath()) {
            LOGGER.error("Payload type {} is not on classpath.", flowLog.getPayloadType().getName());
            return true;
        }
        return false;
    }

    private void terminateFlow(FlowLog flowLog, String reason) {
        try {
            flowLogService.terminate(flowLog.getResourceId(), flowLog.getFlowId(), reason);
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    private boolean isRestartableFlow(FlowLog flowLog) {
        return !FlowConstants.FINISHED_STATE.equals(flowLog.getCurrentState()) && flowLog.getFlowType() != null;
    }

    private void continueFlow(FlowLog flowLog) {
        FlowConfiguration<?> flowConfig = findFlowConfig(flowLog);
        String flowChainType = flowChainLogService.getFlowChainType(flowLog.getFlowChainId());
        Payload payload = deserializePayload(flowLog);
        Flow flow = flowConfig.createFlow(flowLog.getFlowId(), flowLog.getFlowChainId(), flowLog.getResourceId(), flowChainType);
        runningFlows.put(flow, flowLog.getFlowChainId());
        flowStatCache.put(flowLog.getFlowId(), flowLog.getFlowChainId(), flowLog.getResourceId(),
                flowConfig.getFlowOperationType().name(), flow.getFlowConfigClass(), true);
        if (flowLog.getFlowChainId() != null) {
            flowChainHandler.restoreFlowChain(flowLog.getFlowChainId());
        }
        flow.initialize(flowLog.getCurrentState(), deserializeVariables(flowLog));
        RestartAction restartAction = flowConfig.getRestartAction(flowLog.getNextEvent());
        if (restartAction != null) {
            RestartContext restartContext = RestartContext.flowRestart(
                    flowLog.getResourceId(),
                    flowLog.getFlowId(),
                    flowLog.getFlowChainId(),
                    flowLog.getFlowTriggerUserCrn(),
                    flowLog.getOperationType().name(),
                    flowLog.getNextEvent());
            restartAction.restart(restartContext, payload);
        } else {
            terminateFlow(flowLog, "Flow restart is not possible, restart action is missing");
        }
    }

    private boolean isRestartableFlowChain(FlowLog flowLog) {
        if (!FlowConstants.FINISHED_STATE.equals(flowLog.getCurrentState())
                || flowLog.getFlowChainId() == null
                || flowLog.getFlowType() == null) {
            return false;
        }
        return flowChainLogService.findFirstByFlowChainIdOrderByCreatedDesc(flowLog.getFlowChainId())
                .map(flowChainLog -> {
                    List<FlowChainLog> relatedFlowChainLogs;
                    if (flowChainLog.getParentFlowChainId() == null) {
                        relatedFlowChainLogs = List.of(flowChainLog);
                    } else {
                        relatedFlowChainLogs = flowChainLogService.collectRelatedFlowChains(flowChainLog);
                    }
                    return flowChainLogService.hasEventInFlowChainQueue(relatedFlowChainLogs);
                })
                .orElse(false);
    }

    private void continueFlowChain(FlowLog flowLog) {
        flowChainHandler.restoreFlowChain(flowLog.getFlowChainId());
        FlowConfiguration<?> flowConfig = findFlowConfig(flowLog);
        RestartAction restartAction = flowConfig.getRestartAction(null);
        if (restartAction != null) {
            RestartContext restartContext = RestartContext.flowChainRestart(
                    flowLog.getResourceId(),
                    flowLog.getFlowChainId(),
                    flowLog.getFlowTriggerUserCrn(),
                    flowLog.getOperationType().name(),
                    FlowLogUtil.tryDeserializeVariables(flowLog));
            restartAction.restart(restartContext, null);
        } else {
            terminateFlow(flowLog, "Flow chain restart is not possible, restart action is missing");
        }
    }

    private FlowConfiguration<?> findFlowConfig(FlowLog flowLog) {
        return flowConfigs
                .stream()
                .filter(fc -> flowLog.isFlowType(fc.getClass()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Not found flow type " + flowLog.getFlowType()));
    }

    private String getFlowId(Event<?> event) {
        return event.getHeaders().get(FLOW_ID);
    }

    private FlowEventContext getFlowEventContext(Event<? extends Payload> event) {
        return new FlowEventContext(getFlowId(event), getFlowTriggerUserCrn(event), getFlowOperationType(event), getFlowChainId(event),
                event.getKey(), getFlowChainType(event), event.getData());
    }

    private String getFlowChainId(Event<?> event) {
        return event.getHeaders().get(FLOW_CHAIN_ID);
    }

    private String getFlowChainType(Event<?> event) {
        return event.getHeaders().get(FLOW_CHAIN_TYPE);
    }

    private String getFlowOperationType(Event<?> event) {
        return event.getHeaders().get(FLOW_OPERATION_TYPE);
    }

    private String getFlowTriggerUserCrn(Event<?> event) {
        return event.getHeaders().get(FlowConstants.FLOW_TRIGGER_USERCRN);
    }

    private Map<Object, Object> getContextParams(Event<?> event) {
        Map<Object, Object> contextParams = event.getHeaders().get(FlowConstants.FLOW_CONTEXTPARAMS_ID);
        return contextParams == null ? new HashMap<>() : contextParams;
    }

    private void logFlowId(String flowId) {
        String requestId = MDCBuilder.getOrGenerateRequestId();
        LOGGER.debug("Flow has been created with id: '{}' and the related request id: '{}'.", flowId, requestId);
    }

    public FlowLog getFirstRetryableStateLogfromLatestFlow(Long resourceId) {
        return FlowLogUtil.getFirstStateLog(findAllForLastFlowIdChecked(resourceId)).get();
    }

    public List<FlowLog> findAllForLastFlowIdChecked(Long resourceId) {
        List<FlowLog> flowLogs = flowLogService.findAllForLastFlowIdByResourceIdOrderByCreatedDesc(resourceId);
        Optional<FlowLog> pendingFlowLog = FlowLogUtil.getPendingFlowLog(flowLogs);
        if (pendingFlowLog.isPresent()) {
            LOGGER.info("Retry cannot be performed, because there is already an active flow: {}", pendingFlowLog.get());
            throw new BadRequestException("Retry cannot be performed, because there is already an active flow.");
        }
        if (!(FlowLogUtil.isFlowFailHandled(flowLogs, failHandledEvents) || FlowLogUtil.isFlowCancelled(flowLogs))) {
            throw new BadRequestException("Retry cannot be performed, because the last action was successful.");
        }
        if (flowLogs.isEmpty()) {
            LOGGER.info("Retry cannot be performed, because there is no latest flow: {}", resourceId);
            throw new InternalServerErrorException(String.format("Retry cannot be performed, because there is no recent action for resource %s. ", resourceId));
        }
        return flowLogs;
    }
}
