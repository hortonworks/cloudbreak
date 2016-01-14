package com.sequenceiq.cloudbreak.core.flow2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import reactor.bus.Event;
import reactor.bus.EventBus;

public abstract class AbstractAction<S extends FlowState, E extends FlowEvent, C extends CommonContext, P> implements Action<S, E> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAction.class);

    @Inject
    private EventBus eventBus;

    private Class<P> payloadClass;
    private List<PayloadConverter<P>> payloadConverters;

    protected AbstractAction(Class<P> payloadClass) {
        this.payloadClass = payloadClass;
    }

    @PostConstruct
    public void init() {
        payloadConverters = new ArrayList<>();
        initPayloadConverterMap(payloadConverters);
    }

    @Override
    public final void execute(StateContext<S, E> context) {
        P payload = convertPayload(context.getMessageHeader(MessageFactory.HEADERS.DATA.name()));
        C flowContext = createFlowContext(context, payload);
        try {
            doExecute(flowContext, payload, context.getExtendedState().getVariables());
        } catch (Exception ex) {
            FlowEvent failureEvent = context.getStateMachine().getState().getId().failureEvent();
            if (failureEvent != null) {
                sendEvent(flowContext.getFlowId(), failureEvent.stringRepresentation(), getFailurePayload(flowContext, ex));
            } else {
                LOGGER.error("Missing error handling for " + getClass().getSimpleName(), ex);
            }
        }
    }

    protected void sendEvent(String flowId, String selector, Object payload) {
        LOGGER.info("Triggering event: {}", payload);
        Map<String, Object> headers = new HashMap<>();
        headers.put("FLOW_ID", flowId);
        eventBus.notify(selector, new Event(new Event.Headers(headers), payload));
    }

    protected void initPayloadConverterMap(List<PayloadConverter<P>> payloadConverters) {
        // By default payloadconvertermap is empty.
    }

    protected abstract C createFlowContext(StateContext<S, E> stateContext, P payload);

    protected abstract void doExecute(C context, P payload, Map<Object, Object> variables) throws Exception;
    protected abstract Object getFailurePayload(C flowContext, Exception ex);

    private P convertPayload(Object payload) {
        P result = null;
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
        return result;
    }
}
