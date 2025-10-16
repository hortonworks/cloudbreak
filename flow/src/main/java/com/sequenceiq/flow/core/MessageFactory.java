package com.sequenceiq.flow.core;

import java.util.HashMap;
import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

public class MessageFactory<E> {

    public Message<E> createMessage(FlowParameters flowParameters, E key) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(HEADERS.DATA.name(), flowParameters.getPayload());
        headers.put(HEADERS.FLOW_PARAMETERS.name(), flowParameters);
        return new GenericMessage<>(key, headers);
    }

    public enum HEADERS {
        FLOW_ID, FLOW_TRIGGER_USERCRN, DATA, FLOW_PARAMETERS
    }
}
