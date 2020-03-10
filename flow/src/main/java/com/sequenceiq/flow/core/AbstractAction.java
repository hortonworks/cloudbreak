package com.sequenceiq.flow.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

import reactor.bus.EventBus;

public abstract class AbstractAction<S extends FlowState, E extends FlowEvent, C extends CommonContext, P extends Payload> implements Action<S, E> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAction.class);

    private static final String FLOW_START_TIME = "FLOW_START_TIME";

    private static final String FLOW_START_EXEC_TIME = "FLOW_START_EXEC_TIME";

    private static final String FLOW_STATE_NAME = "FLOW_STATE_NAME";

    private static final int MS_PER_SEC = 1000;

    @Inject
    private MetricService metricService;

    @Inject
    private EventBus eventBus;

    @Inject
    private FlowRegister runningFlows;

    @Inject
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    private final Class<P> payloadClass;

    private List<PayloadConverter<P>> payloadConverters;

    private E failureEvent;

    protected AbstractAction(Class<P> payloadClass) {
        this.payloadClass = payloadClass;
    }

    @PostConstruct
    public void init() {
        payloadConverters = new ArrayList<>();
        initPayloadConverterMap(payloadConverters);
    }

    @Override
    public void execute(StateContext<S, E> context) {
        FlowParameters flowParameters = (FlowParameters) context.getMessageHeader(MessageFactory.HEADERS.FLOW_PARAMETERS.name());
        ThreadBasedUserCrnProvider.doAs(flowParameters.getFlowTriggerUserCrn(), () -> {
            MDCBuilder.addFlowId(flowParameters.getFlowId());
            P payload = convertPayload(context.getMessageHeader(MessageFactory.HEADERS.DATA.name()));
            C flowContext = null;
            try {
                Map<Object, Object> variables = context.getExtendedState().getVariables();
                prepareExecution(payload, variables);
                flowContext = createFlowContext(flowParameters, context, payload);
                Object flowStartTime = variables.get(FLOW_START_TIME);
                if (flowStartTime != null) {
                    Object execTime = variables.get(FLOW_START_EXEC_TIME);
                    long flowElapsed = (System.currentTimeMillis() - (long) flowStartTime) / MS_PER_SEC;
                    long execElapsed = (System.currentTimeMillis() - (long) execTime) / MS_PER_SEC;
                    String flowStateName = String.valueOf(variables.get(FLOW_STATE_NAME));
                    long executionTime = Math.max(execElapsed, flowElapsed);
                    LOGGER.debug("Stack: {}, flow state: {}, phase: {}, execution time {} sec", payload.getResourceId(),
                            flowStateName, execElapsed > flowElapsed ? "doExec" : "service", executionTime);
                    metricService.submit(FlowMetricType.FLOW_STEP, executionTime, Map.of("name", flowStateName.toLowerCase()));
                }
                variables.put(FLOW_STATE_NAME, context.getStateMachine().getState().getId());
                variables.put(FLOW_START_EXEC_TIME, System.currentTimeMillis());
                doExecute(flowContext, payload, variables);
                variables.put(FLOW_START_TIME, System.currentTimeMillis());
            } catch (Exception ex) {
//                throw new RuntimeException("shit happened again! :D");
                LOGGER.error("Error during execution of " + getClass().getName(), ex);
                if (failureEvent != null) {
                    sendEvent(flowParameters, failureEvent.event(), getFailurePayload(payload, Optional.ofNullable(flowContext), ex), Map.of());
                } else {
                    LOGGER.error("Missing error handling for " + getClass().getName());
                }
            }
        });
    }

    public void setFailureEvent(E failureEvent) {
        if (this.failureEvent != null && !this.failureEvent.equals(failureEvent)) {
            throw new UnsupportedOperationException("Failure event already configured. Actions reusable not allowed!");
        }
        this.failureEvent = failureEvent;
    }

    public MetricService getMetricService() {
        return metricService;
    }

    protected Flow getFlow(String flowId) {
        return runningFlows.get(flowId);
    }

    protected void sendEvent(C context) {
        Selectable payload = createRequest(context);
        sendEvent(context, payload.selector(), payload);
    }

    protected void sendEvent(CommonContext context, Selectable payload) {
        sendEvent(context, payload.selector(), payload);
    }

    protected void sendEvent(CommonContext context, String selector, Object payload) {
        sendEvent(context.getFlowParameters(), selector, payload, Map.of());
    }

    protected void sendEvent(FlowParameters flowParameters, String selector, Object payload, Map<Object, Object> contextParameters) {
        LOGGER.debug("Triggering event: {}, payload: {}", selector, payload);
        Map<String, Object> headers = new HashMap<>();
        headers.put(FlowConstants.FLOW_ID, flowParameters.getFlowId());
        headers.put(FlowConstants.FLOW_TRIGGER_USERCRN, flowParameters.getFlowTriggerUserCrn());
        String flowChainId = runningFlows.getFlowChainId(flowParameters.getFlowId());
        if (flowChainId != null) {
            headers.put(FlowConstants.FLOW_CHAIN_ID, flowChainId);
        }
        if (!CollectionUtils.isEmpty(contextParameters)) {
            headers.put(FlowConstants.FLOW_CONTEXTPARAMS_ID, contextParameters);
        }
        eventBus.notify(selector, reactorEventFactory.createEvent(headers, payload));
    }

    protected void initPayloadConverterMap(List<PayloadConverter<P>> payloadConverters) {
        // By default payloadConverter map is empty.
    }

    protected void prepareExecution(P payload, Map<Object, Object> variables) {
    }

    protected Selectable createRequest(C context) {
        throw new UnsupportedOperationException("Context based request creation is not supported by default");
    }

    protected abstract C createFlowContext(FlowParameters flowParameters, StateContext<S, E> stateContext, P payload);

    protected abstract void doExecute(C context, P payload, Map<Object, Object> variables) throws Exception;

    protected abstract Object getFailurePayload(P payload, Optional<C> flowContext, Exception ex);

    private P convertPayload(Object payload) {
        P result = null;
        try {
            if (payload == null || payloadClass.isAssignableFrom(payload.getClass())) {
                result = (P) payload;
            } else {
                for (PayloadConverter<P> payloadConverter : payloadConverters) {
                    if (payloadConverter.canConvert(payload.getClass())) {
                        result = payloadConverter.convert(payload);
                        break;
                    }
                }
                if (result == null) {
                    LOGGER.error("No payload converter found for {}, payload will be null", payload);
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Error happened during payload conversion, converted payload will be null! ", ex);
        }
        return result;
    }
}
