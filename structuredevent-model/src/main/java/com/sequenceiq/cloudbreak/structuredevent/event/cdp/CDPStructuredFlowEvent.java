package com.sequenceiq.cloudbreak.structuredevent.event.cdp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.json.Base64Deserializer;
import com.sequenceiq.cloudbreak.structuredevent.json.Base64Serializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CDPStructuredFlowEvent<T> extends CDPStructuredEvent {
    private FlowDetails flow;

    @JsonSerialize(using = Base64Serializer.class)
    @JsonDeserialize(using = Base64Deserializer.class)
    private String exception;

    private T payload;

    public CDPStructuredFlowEvent() {
        super(CDPStructuredFlowEvent.class.getSimpleName());
    }

    public CDPStructuredFlowEvent(String type, CDPOperationDetails operation, FlowDetails flow, T payload) {
        super(type, operation);
        this.flow = flow;
        this.payload = payload;
    }

    @Override
    public String getStatus() {
        String state = flow.getFlowState();
        if ("unknown".equals(state)) {
//            if (stack != null) {
//                state = "STACK_" + stack.getDetailedStatus();
//            }
//            if (cluster != null) {
//                state += " | CLUSTER_" + cluster.getStatus();
//            }
        }
        return state;
    }

    @Override
    public Long getDuration() {
        return Math.max(0L, flow.getDuration());
    }

    public FlowDetails getFlow() {
        return flow;
    }

    public void setFlow(FlowDetails flow) {
        this.flow = flow;
    }

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }
}
