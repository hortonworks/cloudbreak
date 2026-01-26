package com.sequenceiq.flow.core;

import static java.lang.String.format;
import static java.lang.String.valueOf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.transition.Transition;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.FlowEdgeConfig;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.flow.service.flowlog.FlowLogDBService;

public abstract class AbstractAction<S extends FlowState, E extends FlowEvent, C extends CommonContext, P extends Payload> implements Action<S, E> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAction.class);

    private static final String FLOW_START_TIME = "FLOW_START_TIME";

    private static final String FLOW_START_EXEC_TIME = "FLOW_START_EXEC_TIME";

    private static final String FLOW_STATE_NAME = "FLOW_STATE_NAME";

    private static final int MS_PER_SEC = 1000;

    private final Class<P> payloadClass;

    @Inject
    private MetricService metricService;

    @Qualifier("CommonMetricService")
    @Inject
    private MetricService commonMetricsService;

    @Inject
    private EventBus eventBus;

    @Inject
    private FlowRegister runningFlows;

    @Inject
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Inject
    private FlowLogDBService flowLogDBService;

    private List<PayloadConverter<P>> payloadConverters;

    private E failureEvent;

    private FlowEdgeConfig<S, E> flowEdgeConfig;

    private String failureStateId;

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
            String flowId = flowParameters.getFlowId();
            MDCBuilder.addFlowId(flowId);
            P payload = convertPayload(context.getMessageHeader(MessageFactory.HEADERS.DATA.name()));
            C flowContext = null;
            try {
                Map<Object, Object> variables = context.getExtendedState().getVariables();
                prepareExecution(payload, variables);
                String flowStateName = valueOf(variables.get(FLOW_STATE_NAME));
                flowContext = createFlowContext(flowParameters, context, payload);

                if (getTargetStateId(context).filter(targetStateId -> targetStateId.equals(failureStateId)).isPresent()) {
                    getFlow(flowId).setFlowFailed(payload.getException());
                }
                executeAction(context, payload, flowContext, variables, flowStateName);
            } catch (Exception ex) {
                LOGGER.error("Error during execution of {}", getClass().getName(), ex);
                if (failureEvent != null) {
                    try {
                        sendEvent(flowParameters, failureEvent.event(), getFailurePayload(payload, Optional.ofNullable(flowContext), ex), Map.of());
                    } catch (Exception sendEventException) {
                        LOGGER.error("Failed event propagation failed", sendEventException);
                        closeFlowOnError(flowId, ex);
                        throw new CloudbreakServiceException("Failed event propagation failed", sendEventException);
                    }
                } else {
                    if (getTargetStateId(context).filter(targetStateId -> targetStateId.equals(failureStateId)).isPresent()) {
                        closeFlowOnError(flowId, format("Error handler failed in %s state. Message: %s", failureStateId, ex.getMessage()));
                        throw new CloudbreakServiceException(format("Error handler failed in %s state.", failureStateId), ex);
                    } else {
                        closeFlowOnError(flowId, ex);
                        throw new CloudbreakServiceException("Missing error handling for " + getClass().getName(), ex);
                    }
                }
            }
        });
    }

    private boolean isDefaultFailureState(Optional<String> tragetStateId) {
        return flowEdgeConfig != null && flowEdgeConfig.getDefaultFailureState() != null &&
                flowEdgeConfig.getDefaultFailureState().name().equals(tragetStateId.get());
    }

    private Optional<String> getTargetStateId(StateContext<S, E> context) {
        return Optional.of(context).map(StateContext::getTransition)
                .map(Transition::getTarget).map(State::getId).map(Objects::toString);
    }

    private void closeFlowOnError(String flowId, Exception ex) {
        closeFlowOnError(flowId, format("Unhandled exception happened in flow execution, type: %s, message: %s",
                ex.getClass().getName(), ex.getMessage()));
    }

    private void closeFlowOnError(String flowId, String message) {
        if (flowId != null) {
            LOGGER.error("Closing flow with id {}", flowId);
            flowLogDBService.closeFlowOnError(flowId, message);
        }
    }

    private void executeAction(StateContext<S, E> context, P payload, C flowContext, Map<Object, Object> variables, String flowStateName) throws Exception {
        Object flowStartTime = variables.get(FLOW_START_TIME);
        if (flowStartTime != null) {
            Object execTime = variables.get(FLOW_START_EXEC_TIME);
            long flowElapsed = (System.currentTimeMillis() - (long) flowStartTime) / MS_PER_SEC;
            long execElapsed = (System.currentTimeMillis() - (long) execTime) / MS_PER_SEC;
            long executionTime = Math.max(execElapsed, flowElapsed);
            String resourceId = getResourceId(payload, flowStateName);
            LOGGER.debug("Resource ID: {}, flow state: {}, phase: {}, execution time {} sec", resourceId,
                    flowStateName, execElapsed > flowElapsed ? "doExec" : "service", executionTime);
            commonMetricsService.recordTimer(executionTime, FlowMetricType.FLOW_STEP, "name", flowStateName.toLowerCase(Locale.ROOT));
        }
        variables.put(FLOW_STATE_NAME, context.getStateMachine().getState().getId());
        variables.put(FLOW_START_EXEC_TIME, System.currentTimeMillis());
        doExecute(flowContext, payload, variables);
        variables.put(FLOW_START_TIME, System.currentTimeMillis());
    }

    public void setFailureEvent(E failureEvent) {
        if (this.failureEvent != null && !this.failureEvent.equals(failureEvent)) {
            throw new UnsupportedOperationException("Failure event already configured. Actions reusable not allowed!");
        }
        this.failureEvent = failureEvent;
    }

    public void setFlowEdgeConfig(FlowEdgeConfig<S, E> flowEdgeConfig) {
        this.flowEdgeConfig = flowEdgeConfig;
    }

    public void setFailureStateId(String failureStateId) {
        this.failureStateId = failureStateId;
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
        headers.put(FlowConstants.FLOW_OPERATION_TYPE, flowParameters.getFlowOperationType());
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

    protected String getCurrentFlowStateName(Map<Object, Object> variables) {
        return valueOf(variables.get(FLOW_STATE_NAME));
    }

    private String getResourceId(P payload, String flowStateName) {
        String resource;
        if (payload == null) {
            resource = "no payload!";
            LOGGER.warn("No payload in flow state {}", flowStateName);
        } else {
            resource = payload.getResourceId().toString();
        }
        return resource;
    }
}