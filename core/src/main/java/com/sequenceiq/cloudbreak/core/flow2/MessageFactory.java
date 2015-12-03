package com.sequenceiq.cloudbreak.core.flow2;

import java.util.HashMap;
import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

public class MessageFactory<E> {

    public enum HEADERS {
        FLOW_ID, DATA
    }

    public Message<E> createMessage(String flowId, E key, Object data) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(HEADERS.FLOW_ID.name(), flowId);
        headers.put(HEADERS.DATA.name(), data);
        return new GenericMessage<>(key, headers);
    }
}
