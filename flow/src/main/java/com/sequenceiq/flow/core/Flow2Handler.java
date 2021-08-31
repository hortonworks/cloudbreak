package com.sequenceiq.flow.core;

import static com.sequenceiq.flow.core.FlowConstants.FLOW_CANCEL;
import static com.sequenceiq.flow.core.FlowConstants.FLOW_CHAIN_ID;
import static com.sequenceiq.flow.core.FlowConstants.FLOW_CHAIN_TYPE;
import static com.sequenceiq.flow.core.FlowConstants.FLOW_FINAL;
import static com.sequenceiq.flow.core.FlowConstants.FLOW_ID;
import static com.sequenceiq.flow.core.FlowConstants.FLOW_OPERATION_TYPE;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cedarsoftware.util.io.JsonReader;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.common.event.IdempotentEvent;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.flowlog.FlowLogUtil;
import com.sequenceiq.cloudbreak.util.NullUtil;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.cleanup.InMemoryCleanup;
import com.sequenceiq.flow.core.cache.FlowStatCache;
import com.sequenceiq.flow.core.chain.FlowChainHandler;
import com.sequenceiq.flow.core.chain.FlowChains;
import com.sequenceiq.flow.core.config.FlowConfiguration;
import com.sequenceiq.flow.core.exception.FlowNotFoundException;
import com.sequenceiq.flow.core.exception.FlowNotTriggerableException;
import com.sequenceiq.flow.core.model.FlowAcceptResult;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.FlowLogIdWithTypeAndTimestamp;
import com.sequenceiq.flow.ha.NodeConfig;
import com.sequenceiq.flow.service.flowlog.FlowChainLogService;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import reactor.bus.Event;
import reactor.fn.Consumer;

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
    private Tracer tracer;

    @Inject
    private NodeConfig nodeConfig;

    @Inject
    private InMemoryCleanup inMemoryCleanup;

    @Inject
    private FlowChainLogService flowChainLogService;

    @Override
    public void accept(Event<? extends Payload> event) {
        String key = (String) event.getKey();
        Payload payload = event.getData();
        String flowId = getFlowId(event);
        String flowChainId = getFlowChainId(event);
        String flowChainType = getFlowChainType(event);
        String flowTriggerUserCrn = getFlowTriggerUserCrn(event);
        String operationType = getFlowOperationType(event);
        Span activeSpan = tracer.activeSpan();
        SpanContext spanContext = event.getHeaders().get(FlowConstants.SPAN_CONTEXT);
        String operationName = event.getKey().toString();
        if (FlowTracingUtil.isActiveSpanReusable(activeSpan, spanContext, operationName)) {
            LOGGER.debug("Reusing existing span. {}", activeSpan.context());
            doAccept(event, key, payload, flowChainId, flowChainType, new FlowParameters(flowId, flowTriggerUserCrn, operationType, spanContext));
        } else {
            Span span = FlowTracingUtil.getSpan(tracer, operationName, spanContext, flowId, flowChainId, flowTriggerUserCrn);
            spanContext = FlowTracingUtil.useOrCreateSpanContext(spanContext, span);
            try (Scope ignored = tracer.activateSpan(span)) {
                doAccept(event, key, payload, flowChainId, flowChainType, new FlowParameters(flowId, flowTriggerUserCrn, operationType, spanContext));
            } finally {
                span.finish();
            }
        }
    }

    private void doAccept(Event<? extends Payload> event, String key, Payload payload, String flowChainId, String flowChainType,
            FlowParameters flowParameters) {
        try {
            if (FLOW_CANCEL.equals(key)) {
                cancelRunningFlows(payload.getResourceId());
            } else if (FLOW_FINAL.equals(key)) {
                finalizeFlow(flowParameters, flowChainId, payload.getResourceId(), getContextParams(event));
            } else if (flowParameters.getFlowId() == null) {
                AcceptResult result = handleNewFlowRequest(key, payload, flowParameters, flowChainId, flowChainType, getContextParams(event));
                LOGGER.info("Create new flow result {}", result);
                if (isAcceptablePayload(payload)) {
                    ((Acceptable) payload).accepted().accept(result);
                }
            } else {
                handleFlowControlEvent(key, payload, flowParameters, flowChainId);
            }
        } catch (FlowNotTriggerableException e) {
            LOGGER.error("Failed to handle flow event.", e);
            if (isAcceptablePayload(payload)) {
                ((Acceptable) payload).accepted().onError(e);
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
        }
    }

    private void finalizeFlow(FlowParameters flowParameters, String flowChainId, Long stackId, Map<Object, Object> contextParams)
            throws TransactionExecutionException {
        String flowId = flowParameters.getFlowId();
        LOGGER.debug("flow finalizing arrived: id: {}", flowId);
        flowLogService.close(stackId, flowId);
        Flow flow = runningFlows.remove(flowId);
        flowStatCache.remove(flowId, flowChainId == null && !flow.isFlowFailed());
        if (flowChainId != null) {
            if (flow.isFlowFailed()) {
                flowChains.removeFullFlowChain(flowChainId, false);
            } else {
                flowChains.triggerNextFlow(flowChainId, flowParameters.getFlowTriggerUserCrn(), contextParams, flowParameters.getFlowOperationType());
            }
        }
    }

    private AcceptResult handleNewFlowRequest(String key, Payload payload, FlowParameters flowParameters, String flowChainId, String flowChainType,
            Map<Object, Object> contextParams) throws TransactionExecutionException {
        LOGGER.debug("Flow trigger arrived: key: {}, payload: {}", key, payload);
        FlowConfiguration<?> flowConfig = getFlowConfiguration(key);
        FlowTriggerConditionResult flowTriggerConditionResult = flowConfig.getFlowTriggerCondition().isFlowTriggerable(payload.getResourceId());
        if (!flowTriggerConditionResult.isTriggerable()) {
            throw new FlowNotTriggerableException(flowTriggerConditionResult.getErrorMessage());
        } else {
            Set<FlowLogIdWithTypeAndTimestamp> flowLogItems = flowLogService.findAllRunningNonTerminationFlowsByResourceId(payload.getResourceId());
            if (hasRunningAndParallelNotAllowed(key, flowLogItems)) {
                return handleFlowConflict(key, payload, flowChainId, flowLogItems);
            } else {
                return createNewFlow(key, payload, flowParameters, flowChainId, flowChainType, contextParams, flowConfig);
            }
        }
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

    private AcceptResult handleFlowConflict(String key, Payload payload, String flowChainId, Set<FlowLogIdWithTypeAndTimestamp> flowLogItems) {
        AcceptResult acceptResult = null;
        Optional<FlowLog> initFlowLog = flowLogService.findAllByFlowIdOrderByCreatedDesc(flowLogItems.iterator().next().getFlowId())
                .stream()
                .sorted(Comparator.comparing(FlowLog::getCreated))
                .findFirst();
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
                if (isIdempotentTriggers(previousTrigger, payload)) {
                    LOGGER.info("Idempotent flow trigger. Running {}, requested {}", previousTrigger, payload);
                    acceptResult = FlowAcceptResult.runningInFlow(initFlowLog.get().getFlowId());
                }
            }
        }
        if (acceptResult == null) {
            LOGGER.info("Flow operation not allowed, other flow is running. Resource ID {}, event {}", payload.getResourceId(), key);
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
        return ((IdempotentEvent) currentTrigger).equalsEvent((IdempotentEvent) previousTrigger);
    }

    private FlowAcceptResult createNewFlow(String key, Payload payload, FlowParameters flowParameters, String flowChainId, String flowChainType,
            Map<Object, Object> contextParams, FlowConfiguration<?> flowConfig) throws TransactionExecutionException {
        String flowId = UUID.randomUUID().toString();
        addFlowParameters(flowParameters, flowId, flowChainId, flowConfig);
        Flow flow = flowConfig.createFlow(flowId, flowChainId, payload.getResourceId(), flowChainType);
        try {
            flow.initialize(contextParams);
            runningFlows.put(flow, flowChainId);
            flowStatCache.put(flowId, flowChainId, payload.getResourceId(),
                    flowConfig.getFlowOperationType().name(), flow.getFlowConfigClass(), false);
            transactionService.required(() -> {
                flowLogService.save(flowParameters, flowChainId, key, payload, null, flowConfig.getClass(), flow.getCurrentState());
                if (flowChainId != null) {
                    flowChains.saveAllUnsavedFlowChains(flowChainId, flowParameters.getFlowTriggerUserCrn());
                    flowChains.removeLastTriggerEvent(flowChainId, flowParameters.getFlowTriggerUserCrn());
                }
            });
            logFlowId(flowId);
            flow.sendEvent(key, flowParameters.getFlowTriggerUserCrn(), payload, flowParameters.getSpanContext(),
                    flowParameters.getFlowOperationType());
            return getFlowAcceptResult(flowChainId, flowParameters.getFlowId());
        } catch (Exception e) {
            LOGGER.error("Can't save flow: {}", flowId);
            runningFlows.remove(flowId);
            flowStatCache.remove(flowId, false);
            if (flowChainId != null) {
                flowChains.removeFullFlowChain(flowChainId, false);
            }
            flow.stop();
            throw e;
        }
    }

    private void addFlowParameters(FlowParameters flowParameters, String flowId, String flowChainId, FlowConfiguration<?> flowConfig) {
        flowParameters.setFlowId(flowId);
        if (flowChainId == null) {
            flowParameters.setFlowOperationType(flowConfig.getFlowOperationType().name());
        }
    }

    private FlowAcceptResult getFlowAcceptResult(String flowChainId, String flowId) {
        FlowAcceptResult acceptResult;
        if (flowChainId != null) {
            acceptResult = FlowAcceptResult.runningInFlowChain(flowChainId);
        } else {
            acceptResult = FlowAcceptResult.runningInFlow(flowId);
        }
        return acceptResult;
    }

    private void handleFlowControlEvent(String key, Payload payload, FlowParameters flowParameters, String flowChainId) throws
            TransactionExecutionException {
        String flowId = flowParameters.getFlowId();
        LOGGER.debug("flow control event arrived: key: {}, flowid: {}, usercrn: {}, payload: {}", key, flowId, flowParameters.getFlowTriggerUserCrn(), payload);
        Flow flow = runningFlows.get(flowId);
        if (flow != null) {
            MutableBoolean flowCancelled = new MutableBoolean(false);
            try {
                updateFlowLogStatusInTransaction(key, payload, flowParameters, flowChainId, flow, flowCancelled);
            } catch (TransactionExecutionException e) {
                LOGGER.error("Can't update flow status: {}", flowId);
                throw e;
            }
            if (!flowCancelled.booleanValue()) {
                flow.sendEvent(key, flowParameters.getFlowTriggerUserCrn(), payload, flowParameters.getSpanContext(), flowParameters.getFlowOperationType());
            }
        } else {
            LOGGER.debug("Cancelled flow finished running. Resource ID {}, flow ID {}, event {}", payload.getResourceId(), flowId, key);
        }
    }

    private void updateFlowLogStatusInTransaction(String key, Payload payload, FlowParameters flowParameters, String flowChainId, Flow flow,
            MutableBoolean flowCancelled) throws TransactionExecutionException {
        transactionService.required(() -> {
            Optional<FlowLog> lastFlowLog = flowLogService.getLastFlowLog(flow.getFlowId());
            lastFlowLog.ifPresent(flowLog -> {
                String nodeId = nodeConfig.getId();
                if (flowLog.getFinalized() || flowLog.getCloudbreakNodeId() == null || flowLog.getCloudbreakNodeId().equals(nodeId)) {
                    updateFlowLogStatus(key, payload, flowChainId, flow, flowLog, flowParameters);
                } else {
                    LOGGER.info("Flow {} was handled by another node {}, current node ID is {}, abandoning.",
                            flow.getFlowId(), flowLog.getCloudbreakNodeId(), nodeId);
                    inMemoryCleanup.cancelFlowWithoutDbUpdate(flow.getFlowId());
                    flowCancelled.setTrue();
                }
            });
        });
    }

    private void updateFlowLogStatus(String key, Payload payload, String flowChainId, Flow flow, FlowLog lastFlowLog, FlowParameters flowParameters) {
        if (flowLogService.repeatedFlowState(lastFlowLog, key)) {
            flowLogService.updateLastFlowLogPayload(lastFlowLog, payload, flow.getVariables());
        } else {
            flowLogService.updateLastFlowLogStatus(lastFlowLog, failHandledEvents.contains(key));
            flowLogService.save(flowParameters, flowChainId, key, payload, flow.getVariables(), flow.getFlowConfigClass(), flow.getCurrentState());
        }
    }

    private boolean isAcceptablePayload(Payload payload) {
        return payload instanceof Acceptable
                && ((Acceptable) payload).accepted() != null
                && !((Acceptable) payload).accepted().isComplete();
    }

    public FlowIdentifier retryLastFailedFlow(Long resourceId, java.util.function.Consumer<FlowLog> beforeRestart) {
        List<FlowLog> flowLogs = flowLogService.findAllForLastFlowIdByResourceIdOrderByCreatedDesc(resourceId);
        Optional<FlowLog> pendingFlowLog = FlowLogUtil.getPendingFlowLog(flowLogs);
        if (pendingFlowLog.isPresent()) {
            LOGGER.info("Retry cannot be performed, because there is already an active flow: {}", pendingFlowLog.get());
            throw new BadRequestException("Retry cannot be performed, because there is already an active flow.");
        }
        if (!FlowLogUtil.isFlowFailHandled(flowLogs, failHandledEvents)) {
            throw new BadRequestException("Retry cannot be performed, because the last action was successful.");
        }
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

    public void restartFlow(String flowId) {
        FlowLog flowLog = flowLogService.findFirstByFlowIdOrderByCreatedDesc(flowId).orElseThrow(() -> new FlowNotFoundException(flowId));
        restartFlow(flowLog);
    }

    public void restartFlow(FlowLog flowLog) {
        if (flowLog.getFlowType() != null) {
            if (applicationFlowInformation.getRestartableFlows().contains(flowLog.getFlowType())) {
                Optional<FlowConfiguration<?>> flowConfig = flowConfigs.stream()
                        .filter(fc -> fc.getClass().equals(flowLog.getFlowType())).findFirst();
                try {
                    String flowChainType = flowChainLogService.getFlowChainType(flowLog.getFlowChainId());
                    Payload payload = (Payload) JsonReader.jsonToJava(flowLog.getPayload());
                    Flow flow = flowConfig.get().createFlow(flowLog.getFlowId(), flowLog.getFlowChainId(), payload.getResourceId(), flowChainType);
                    runningFlows.put(flow, flowLog.getFlowChainId());
                    flowStatCache.put(flow.getFlowId(), flowLog.getFlowChainId(), payload.getResourceId(),
                            flowConfig.get().getFlowOperationType().name(), flow.getFlowConfigClass(), true);
                    if (flowLog.getFlowChainId() != null) {
                        flowChainHandler.restoreFlowChain(flowLog.getFlowChainId());
                    }
                    Map<Object, Object> variables = (Map<Object, Object>) JsonReader.jsonToJava(flowLog.getVariables());
                    flow.initialize(flowLog.getCurrentState(), variables);
                    RestartAction restartAction = flowConfig.get().getRestartAction(flowLog.getNextEvent());
                    if (restartAction != null) {
                        LOGGER.debug("Restarting flow with id: '{}', flow chain id: '{}', flow type: '{}', restart action: '{}'", flow.getFlowId(),
                                flowLog.getFlowChainId(), flowLog.getFlowType().getSimpleName(), restartAction.getClass().getSimpleName());
                        Span span = tracer.buildSpan(flowLog.getCurrentState()).ignoreActiveSpan().start();
                        restartAction.restart(new FlowParameters(flowLog.getFlowId(), flowLog.getFlowTriggerUserCrn(),
                                flowLog.getOperationType().name(), span.context()), flowLog.getFlowChainId(), flowLog.getNextEvent(), payload);
                        return;
                    }
                } catch (RuntimeException e) {
                    String message = String.format("Flow could not be restarted with id: '%s', flow chain id: '%s' and flow type: '%s'", flowLog.getFlowId(),
                            flowLog.getFlowChainId(), flowLog.getFlowType().getSimpleName());
                    LOGGER.error(message, e);
                }
            }
            try {
                flowLogService.terminate(flowLog.getResourceId(), flowLog.getFlowId());
            } catch (TransactionExecutionException e) {
                throw new TransactionRuntimeExecutionException(e);
            }
        }
    }

    private String getFlowId(Event<?> event) {
        return event.getHeaders().get(FLOW_ID);
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
        return contextParams == null ? Map.of() : contextParams;
    }

    private void logFlowId(String flowId) {
        String requestId = MDCBuilder.getOrGenerateRequestId();
        LOGGER.debug("Flow has been created with id: '{}' and the related request id: '{}'.", flowId, requestId);
    }
}
