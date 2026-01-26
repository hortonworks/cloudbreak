package com.sequenceiq.flow.core;

import java.util.HashMap;
import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

public class MessageFactory<E> {

    public Message<E> createMessage(FlowEventContext flowEventContext, E key) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(HEADERS.DATA.name(), flowEventContext.getPayload());
        FlowParameters flowParameters = new FlowParameters(flowEventContext.getFlowId(), flowEventContext.getFlowTriggerUserCrn(),
                flowEventContext.getFlowOperationType());
        headers.put(HEADERS.FLOW_PARAMETERS.name(), flowParameters);
        return new GenericMessage<>(key, headers);
    }

    public enum HEADERS {
        DATA, FLOW_PARAMETERS
    }
}
