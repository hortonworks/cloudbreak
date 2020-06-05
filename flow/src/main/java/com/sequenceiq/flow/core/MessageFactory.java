package com.sequenceiq.flow.core;

import java.util.HashMap;
import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import io.opentracing.SpanContext;

public class MessageFactory<E> {

    public enum HEADERS {
        FLOW_ID, FLOW_TRIGGER_USERCRN, DATA, FLOW_PARAMETERS
    }

    public Message<E> createMessage(String flowId, String flowTriggerUserCrn, E key, Object data, SpanContext spanContext) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(HEADERS.DATA.name(), data);
        headers.put(HEADERS.FLOW_PARAMETERS.name(), new FlowParameters(flowId, flowTriggerUserCrn, spanContext));
        return new GenericMessage<>(key, headers);
    }
}
