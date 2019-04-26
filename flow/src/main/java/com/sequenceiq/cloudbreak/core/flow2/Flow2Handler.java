package com.sequenceiq.cloudbreak.core.flow2;

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
import com.sequenceiq.cloudbreak.cloud.Acceptable;
import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainHandler;
import com.sequenceiq.cloudbreak.core.flow2.chain.FlowChains;
import com.sequenceiq.cloudbreak.core.flow2.config.FlowConfiguration;
import com.sequenceiq.cloudbreak.core.flow2.exception.FlowNotFoundException;
import com.sequenceiq.cloudbreak.domain.FlowLog;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;

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

        try {
            handle(key, payload, flowId, flowChainId);
        } catch (TransactionExecutionException e) {
            LOGGER.error("Failed update last flow log status and save new flow log entry.", e);
            runningFlows.remove(flowId);
        }
    }

    private void handle(String key, Payload payload, String flowId, String flowChainId) throws TransactionExecutionException {
        switch (key) {
            case FLOW_CANCEL:
                cancelRunningFlows(payload.getStackId());
                break;
            case FLOW_FINAL:
                finalizeFlow(flowId, flowChainId, payload.getStackId());
                break;
            default:
                if (flowId == null) {
                    LOGGER.debug("flow trigger arrived: key: {}, payload: {}", key, payload);
                    FlowConfiguration<?> flowConfig = flowConfigurationMap.get(key);
                    if (flowConfig != null && flowConfig.getFlowTriggerCondition().isFlowTriggerable(payload.getStackId())) {
                        if (!isFlowAcceptable(key, payload)) {
                            LOGGER.info("Flow operation not allowed, other flow is running. Stack ID {}, event {}", payload.getStackId(), key);
                            return;
                        }
                        flowId = UUID.randomUUID().toString();
                        Flow flow = flowConfig.createFlow(flowId, payload.getStackId());
                        flow.initialize();
                        flowLogService.save(flowId, flowChainId, key, payload, null, flowConfig.getClass(), flow.getCurrentState());
                        acceptFlow(payload);
                        logFlowId(flowId);
                        runningFlows.put(flow, flowChainId);
                        flow.sendEvent(key, payload);
                    }
                } else {
                    handleFlowControlEvent(key, payload, flowId, flowChainId);
                }
                break;
        }
    }

    private void handleFlowControlEvent(String key, Payload payload, String flowId, String flowChainId) throws TransactionExecutionException {
        LOGGER.debug("flow control event arrived: key: {}, flowid: {}, payload: {}", key, flowId, payload);
        Flow flow = runningFlows.get(flowId);
        if (flow != null) {
            transactionService.required(() -> {
                Optional<FlowLog> lastFlowLog = flowLogService.getLastFlowLog(flow.getFlowId());
                lastFlowLog.ifPresent(flowLog -> updateFlowLogStatus(key, payload, flowChainId, flow, flowLog));
                return null;
            });
            flow.sendEvent(key, payload);
        } else {
            LOGGER.debug("Cancelled flow finished running. Stack ID {}, flow ID {}, event {}", payload.getStackId(), flowId, key);
        }
    }

    private void updateFlowLogStatus(String key, Payload payload, String flowChainId, Flow flow, FlowLog lastFlowLog) {
        if (flowLogService.repeatedFlowState(lastFlowLog, key)) {
            flowLogService.updateLastFlowLogPayload(lastFlowLog, payload, flow.getVariables());
        } else {
            flowLogService.updateLastFlowLogStatus(lastFlowLog, failHandledEvents.contains(key));
            flowLogService.save(flow.getFlowId(), flowChainId, key, payload, flow.getVariables(),
                    flow.getFlowConfigClass(), flow.getCurrentState());
        }
    }

    private boolean isFlowAcceptable(String key, Payload payload) {
        if (payload instanceof Acceptable && ((Acceptable) payload).accepted() != null) {
            Acceptable acceptable = (Acceptable) payload;
            if (!applicationFlowInformation.getAllowedParallelFlows().contains(key) && flowLogService.isOtherFlowRunning(payload.getStackId())) {
                acceptable.accepted().accept(Boolean.FALSE);
                return false;
            }
        }
        return true;
    }

    private void acceptFlow(Payload payload) {
        if (payload instanceof Acceptable && ((Acceptable) payload).accepted() != null) {
            Acceptable acceptable = (Acceptable) payload;
            if (!acceptable.accepted().isComplete()) {
                acceptable.accepted().accept(Boolean.TRUE);
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

    private void finalizeFlow(String flowId, String flowChainId, Long stackId) throws TransactionExecutionException {
        LOGGER.debug("flow finalizing arrived: id: {}", flowId);
        flowLogService.close(stackId, flowId);
        Flow flow = runningFlows.remove(flowId);
        if (flowChainId != null) {
            if (flow.isFlowFailed()) {
                flowChains.removeFullFlowChain(flowChainId);
            } else {
                flowChains.triggerNextFlow(flowChainId);
            }
        }
    }

    public void restartFlow(String flowId) {
        FlowLog flowLog = flowLogService.findFirstByFlowIdOrderByCreatedDesc(flowId).orElseThrow(() -> new FlowNotFoundException(flowId));
        restartFlow(flowLog);
    }

    public void restartFlow(FlowLog flowLog) {
        if (applicationFlowInformation.getRestartableFlows().contains(flowLog.getFlowType())) {
            Optional<FlowConfiguration<?>> flowConfig = flowConfigs.stream()
                    .filter(fc -> fc.getClass().equals(flowLog.getFlowType())).findFirst();
            Payload payload = (Payload) JsonReader.jsonToJava(flowLog.getPayload());
            Flow flow = flowConfig.get().createFlow(flowLog.getFlowId(), payload.getStackId());
            runningFlows.put(flow, flowLog.getFlowChainId());
            if (flowLog.getFlowChainId() != null) {
                flowChainHandler.restoreFlowChain(flowLog.getFlowChainId());
            }
            Map<Object, Object> variables = (Map<Object, Object>) JsonReader.jsonToJava(flowLog.getVariables());
            flow.initialize(flowLog.getCurrentState(), variables);
            RestartAction restartAction = flowConfig.get().getRestartAction(flowLog.getNextEvent());
            if (restartAction != null) {
                restartAction.restart(flowLog.getFlowId(), flowLog.getFlowChainId(), flowLog.getNextEvent(), payload);
                return;
            }
        }
        try {
            flowLogService.terminate(flowLog.getStackId(), flowLog.getFlowId());
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    private String getFlowId(Event<?> event) {
        return event.getHeaders().get(FLOW_ID);
    }

    private String getFlowChainId(Event<?> event) {
        return event.getHeaders().get(FLOW_CHAIN_ID);
    }

    private void logFlowId(String flowId) {
        String requestId = MDCBuilder.getMdcContextMap().get(LoggerContextKey.REQUEST_ID.toString());
        LOGGER.debug("Flow has been created with id: '{}' and the related request id: '{}'.", flowId, requestId);
    }
}
