package com.sequenceiq.flow.core;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cedarsoftware.util.io.JsonReader;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.cleanup.InMemoryCleanup;
import com.sequenceiq.flow.core.chain.FlowChainHandler;
import com.sequenceiq.flow.core.chain.FlowChains;
import com.sequenceiq.flow.core.config.FlowConfiguration;
import com.sequenceiq.flow.core.exception.FlowNotFoundException;
import com.sequenceiq.flow.core.model.FlowAcceptResult;
import com.sequenceiq.flow.core.cache.FlowStatCache;
import com.sequenceiq.flow.domain.FlowLog;
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
    public static final String FLOW_ID = FlowConstants.FLOW_ID;

    public static final String FLOW_CHAIN_ID = FlowConstants.FLOW_CHAIN_ID;

    public static final String FLOW_CHAIN_TYPE = FlowConstants.FLOW_CHAIN_TYPE;

    public static final String FLOW_FINAL = FlowConstants.FLOW_FINAL;

    public static final String FLOW_CANCEL = FlowConstants.FLOW_CANCEL;

    public static final String FLOW_OPERATION_TYPE = FlowConstants.FLOW_OPERATION_TYPE;

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
            doAccept(event, key, payload, flowId, flowChainId, flowChainType, new FlowParameters(flowId, flowTriggerUserCrn, operationType, spanContext));
        } else {
            Span span = FlowTracingUtil.getSpan(tracer, operationName, spanContext, flowId, flowChainId, flowTriggerUserCrn);
            spanContext = FlowTracingUtil.useOrCreateSpanContext(spanContext, span);
            try (Scope ignored = tracer.activateSpan(span)) {
                doAccept(event, key, payload, flowId, flowChainId, flowChainType, new FlowParameters(flowId, flowTriggerUserCrn, operationType, spanContext));
            } finally {
                span.finish();
            }
        }
    }

    private void doAccept(Event<? extends Payload> event, String key, Payload payload, String flowId, String flowChainId, String flowChainType,
            FlowParameters flowParameters) {
        Map<Object, Object> contextParams = getContextParams(event);
        try {
            handle(key, payload, flowParameters, flowChainId, flowChainType, contextParams);
        } catch (Exception e) {
            LOGGER.error("Failed to handle flow event.", e);
            throw new CloudbreakServiceException(e);
        }
    }

    private void handle(String key, Payload payload, FlowParameters flowParameters, String flowChainId, String flowChainType, Map<Object, Object> contextParams)
            throws TransactionExecutionException {
        switch (key) {
            case FLOW_CANCEL:
                cancelRunningFlows(payload.getResourceId());
                break;
            case FLOW_FINAL:
                finalizeFlow(flowParameters, flowChainId, payload.getResourceId(), contextParams);
                break;
            default:
                String flowId = flowParameters.getFlowId();
                if (flowId == null) {
                    LOGGER.debug("flow trigger arrived: key: {}, payload: {}", key, payload);
                    FlowConfiguration<?> flowConfig = flowConfigurationMap.get(key);
                    if (flowConfig != null && flowConfig.getFlowTriggerCondition().isFlowTriggerable(payload.getResourceId())) {
                        if (!isFlowAcceptable(key, payload)) {
                            LOGGER.info("Flow operation not allowed, other flow is running. Stack ID {}, event {}", payload.getResourceId(), key);
                            return;
                        }
                        flowId = UUID.randomUUID().toString();
                        FlowAcceptResult acceptResult;
                        if (flowChainId != null) {
                            acceptResult = FlowAcceptResult.runningInFlowChain(flowChainId);
                        } else {
                            flowParameters.setFlowOperationType(flowConfig.getFlowOperationType().name());
                            acceptResult = FlowAcceptResult.runningInFlow(flowId);
                        }
                        flowParameters.setFlowId(flowId);
                        Flow flow = flowConfig.createFlow(flowId, flowChainId, payload.getResourceId(), flowChainType);
                        flow.initialize(contextParams);
                        runningFlows.put(flow, flowChainId);
                        flowStatCache.put(flowId, flowChainId, payload.getResourceId(),
                                flowConfig.getFlowOperationType().name(), flow.getFlowConfigClass(), false);
                        try {
                            transactionService.required(() -> {
                                flowLogService.save(flowParameters, flowChainId, key, payload, null, flowConfig.getClass(), flow.getCurrentState());
                                if (flowChainId != null) {
                                    flowChains.removeLastTriggerEvent(flowChainId, flowParameters.getFlowTriggerUserCrn());
                                }
                            });
                        } catch (Exception e) {
                            LOGGER.error("Can't save flow: {}", flowId);
                            runningFlows.remove(flowId);
                            flowStatCache.remove(flowId, false);
                            throw e;
                        }
                        acceptFlow(payload, acceptResult);
                        logFlowId(flowId);
                        flow.sendEvent(key, flowParameters.getFlowTriggerUserCrn(), payload,
                                flowParameters.getSpanContext(), flowParameters.getFlowOperationType());
                    }
                } else {
                    handleFlowControlEvent(key, payload, flowParameters, flowChainId);
                }
                break;
        }
    }

    private void handleFlowControlEvent(String key, Payload payload, FlowParameters flowParameters, String flowChainId) throws TransactionExecutionException {
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
            LOGGER.debug("Cancelled flow finished running. Stack ID {}, flow ID {}, event {}", payload.getResourceId(), flowId, key);
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

    private boolean isFlowAcceptable(String key, Payload payload) {
        if (payload instanceof Acceptable && ((Acceptable) payload).accepted() != null) {
            Acceptable acceptable = (Acceptable) payload;
            if (!applicationFlowInformation.getAllowedParallelFlows().contains(key)
                    && flowLogService.isOtherNonTerminationFlowRunning(payload.getResourceId())) {
                acceptable.accepted().accept(FlowAcceptResult.alreadyExistingFlow());
                return false;
            }
        }
        return true;
    }

    private void acceptFlow(Payload payload, AcceptResult acceptResult) {
        if (payload instanceof Acceptable && ((Acceptable) payload).accepted() != null) {
            Acceptable acceptable = (Acceptable) payload;
            if (!acceptable.accepted().isComplete()) {
                acceptable.accepted().accept(acceptResult);
            }
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
        flowStatCache.remove(flowId, true);
        if (flowChainId != null) {
            if (flow.isFlowFailed()) {
                flowChains.removeFullFlowChain(flowChainId, false);
            } else {
                flowChains.triggerNextFlow(flowChainId, flowParameters.getFlowTriggerUserCrn(), contextParams, flowParameters.getFlowOperationType());
            }
        }
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
        String requestId = MDCBuilder.getMdcContextMap().get(LoggerContextKey.REQUEST_ID.toString());
        LOGGER.debug("Flow has been created with id: '{}' and the related request id: '{}'.", flowId, requestId);
    }
}
