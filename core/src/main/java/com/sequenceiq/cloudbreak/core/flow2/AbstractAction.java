package com.sequenceiq.cloudbreak.core.flow2;

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

import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.cloud.event.Selectable;

import reactor.bus.Event;
import reactor.bus.EventBus;

public abstract class AbstractAction<S extends FlowState, E extends FlowEvent, C extends CommonContext, P extends Payload> implements Action<S, E> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAction.class);

    @Inject
    private EventBus eventBus;
    @Inject
    private FlowRegister runningFlows;

    private Class<P> payloadClass;
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
        String flowId = (String) context.getMessageHeader(MessageFactory.HEADERS.FLOW_ID.name());
        P payload = convertPayload(context.getMessageHeader(MessageFactory.HEADERS.DATA.name()));
        C flowContext = null;
        try {
            prepareExecution(context.getExtendedState().getVariables());
            flowContext = createFlowContext(flowId, context, payload);
            doExecute(flowContext, payload, context.getExtendedState().getVariables());
        } catch (Exception ex) {
            LOGGER.error("Error during execution of " + getClass().getSimpleName(), ex);
            if (failureEvent != null) {
                sendEvent(flowId, failureEvent.stringRepresentation(), getFailurePayload(payload, Optional.ofNullable(flowContext), ex));
            } else {
                LOGGER.error("Missing error handling for " + getClass().getSimpleName());
            }
        }
    }

    public void setFailureEvent(E failureEvent) {
        if (this.failureEvent != null && !this.failureEvent.equals(failureEvent)) {
            throw new UnsupportedOperationException("Failure event already configured. Actions reusable not allowed!");
        }
        this.failureEvent = failureEvent;
    }

    protected void sendEvent(C context) {
        Selectable payload = createRequest(context);
        sendEvent(context.getFlowId(), payload.selector(), payload);
    }

    protected void sendEvent(String flowId, Selectable payload) {
        sendEvent(flowId, payload.selector(), payload);
    }

    protected void sendEvent(String flowId, String selector, Object payload) {
        LOGGER.info("Triggering event: {}", payload);
        Map<String, Object> headers = new HashMap<>();
        headers.put("FLOW_ID", flowId);
        eventBus.notify(selector, new Event<>(new Event.Headers(headers), payload));
    }

    protected void initPayloadConverterMap(List<PayloadConverter<P>> payloadConverters) {
        // By default payloadconvertermap is empty.
    }

    protected void prepareExecution(Map<Object, Object> variables) {
    }

    protected abstract C createFlowContext(String flowId, StateContext<S, E> stateContext, P payload);

    protected abstract void doExecute(C context, P payload, Map<Object, Object> variables) throws Exception;
    protected abstract Selectable createRequest(C context);
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
            }
        } catch (Exception ex) {
            LOGGER.error("Error happened during payload conversion, converted payload will be null! ", ex);
        }
        return result;
    }
}
