package com.sequenceiq.flow.core;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cedarsoftware.util.io.JsonReader;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.core.chain.FlowChainHandler;
import com.sequenceiq.flow.core.chain.FlowChains;
import com.sequenceiq.flow.core.config.FlowConfiguration;
import com.sequenceiq.flow.core.exception.FlowNotFoundException;
import com.sequenceiq.flow.core.model.FlowAcceptResult;
import com.sequenceiq.flow.domain.FlowLog;

import reactor.bus.Event;
import reactor.fn.Consumer;

@Component
public class Flow2Handler implements Consumer<Event<? extends Payload>> {
    public static final String FLOW_ID = FlowConstants.FLOW_ID;

    public static final String FLOW_CHAIN_ID = FlowConstants.FLOW_CHAIN_ID;

    public static final String FLOW_FINAL = FlowConstants.FLOW_FINAL;

    public static final String FLOW_CANCEL = FlowConstants.FLOW_CANCEL;

    private static final Logger LOGGER = LoggerFactory.getLogger(Flow2Handler.class);

    @Inject
    private FlowLogService flowLogService;

    @Resource
    private List<FlowConfiguration<?>> flowConfigs;

    @Resource
    private Map<String, FlowConfiguration<?>> flowConfigurationMap;

    @Resource
    private List<String> failHandledEvents;

    @Inject
    private FlowChains flowChains;

    @Inject
    private FlowChainHandler flowChainHandler;

    @Inject
    private FlowRegister runningFlows;

    @Inject
    private TransactionService transactionService;

    @Inject
    private ApplicationFlowInformation applicationFlowInformation;

    @Override
    public void accept(Event<? extends Payload> event) {
        String key = (String) event.getKey();
        Payload payload = event.getData();
        String flowId = getFlowId(event);
        String flowChainId = getFlowChainId(event);
        String flowTriggerUserCrn = getFlowTriggerUserCrn(event);
        FlowParameters flowParameters = new FlowParameters(flowId, flowTriggerUserCrn);
        Map<Object, Object> contextParams = getContextParams(event);
        try {
            handle(key, payload, flowParameters, flowChainId, contextParams);
        } catch (Exception e) {
            LOGGER.error("Failed update last flow log status and save new flow log entry.", e);
            runningFlows.remove(flowId);
        }
    }

    private void handle(String key, Payload payload, FlowParameters flowParameters, String flowChainId, Map<Object, Object> contextParams)
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
                            acceptResult = FlowAcceptResult.runningInFlow(flowId);
                        }
                        flowParameters.setFlowId(flowId);
                        Flow flow = flowConfig.createFlow(flowId, payload.getResourceId());
                        flow.initialize(contextParams);
                        runningFlows.put(flow, flowChainId);
                        try {
                            flowLogService.save(flowParameters, flowChainId, key, payload, null, flowConfig.getClass(), flow.getCurrentState());
                        } catch (Exception e) {
                            LOGGER.error("Can't save flow: {}", flowId);
                            runningFlows.remove(flowId);
                            throw e;
                        }
                        acceptFlow(payload, acceptResult);
                        logFlowId(flowId);
                        flow.sendEvent(key, flowParameters.getFlowTriggerUserCrn(), payload);
                    }
                } else {
                    handleFlowControlEvent(key, payload, flowParameters, flowChainId);
                }
                break;
        }
    }

    private void handleFlowControlEvent(String key, Payload payload, FlowParameters flowParameters, String flowChainId)
            throws TransactionExecutionException {
        String flowId = flowParameters.getFlowId();
        LOGGER.debug("flow control event arrived: key: {}, flowid: {}, usercrn: {}, payload: {}", key, flowId, flowParameters.getFlowTriggerUserCrn(), payload);
        Flow flow = runningFlows.get(flowId);
        if (flow != null) {
            transactionService.required(() -> {
                Optional<FlowLog> lastFlowLog = flowLogService.getLastFlowLog(flow.getFlowId());
                lastFlowLog.ifPresent(flowLog -> updateFlowLogStatus(key, payload, flowChainId, flow, flowLog, flowParameters));
                return null;
            });
            flow.sendEvent(key, flowParameters.getFlowTriggerUserCrn(), payload);
        } else {
            LOGGER.debug("Cancelled flow finished running. Stack ID {}, flow ID {}, event {}", payload.getResourceId(), flowId, key);
        }
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
            String flowChainId = runningFlows.getFlowChainId(id);
            if (flowChainId != null) {
                flowChains.removeFullFlowChain(flowChainId);
            }
            Flow flow = runningFlows.remove(id);
            if (flow != null) {
                flow.stop();
                flowLogService.cancel(stackId, id);
            }
        }
    }

    private void finalizeFlow(FlowParameters flowParameters, String flowChainId, Long stackId, Map<Object, Object> contextParams)
            throws TransactionExecutionException {
        String flowId = flowParameters.getFlowId();
        LOGGER.debug("flow finalizing arrived: id: {}", flowId);
        flowLogService.close(stackId, flowId);
        Flow flow = runningFlows.remove(flowId);
        if (flowChainId != null) {
            if (flow.isFlowFailed()) {
                flowChains.removeFullFlowChain(flowChainId);
            } else {
                flowChains.triggerNextFlow(flowChainId, flowParameters.getFlowTriggerUserCrn(), contextParams);
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
                    Payload payload = (Payload) JsonReader.jsonToJava(flowLog.getPayload());
                    Flow flow = flowConfig.get().createFlow(flowLog.getFlowId(), payload.getResourceId());
                    runningFlows.put(flow, flowLog.getFlowChainId());
                    if (flowLog.getFlowChainId() != null) {
                        flowChainHandler.restoreFlowChain(flowLog.getFlowChainId());
                    }
                    Map<Object, Object> variables = (Map<Object, Object>) JsonReader.jsonToJava(flowLog.getVariables());
                    flow.initialize(flowLog.getCurrentState(), variables);
                    RestartAction restartAction = flowConfig.get().getRestartAction(flowLog.getNextEvent());
                    if (restartAction != null) {
                        restartAction.restart(new FlowParameters(flowLog.getFlowId(), flowLog.getFlowTriggerUserCrn()), flowLog.getFlowChainId(),
                                flowLog.getNextEvent(), payload);
                        return;
                    }
                } catch (RuntimeException e) {
                    LOGGER.error("Can not read payload", e);
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
