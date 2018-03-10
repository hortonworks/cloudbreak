package com.sequenceiq.cloudbreak.structuredevent.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sequenceiq.cloudbreak.structuredevent.json.Base64Deserializer;
import com.sequenceiq.cloudbreak.structuredevent.json.Base64Serializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StructuredFlowErrorEvent extends StructuredFlowEvent {
    @JsonSerialize(using = Base64Serializer.class)
    @JsonDeserialize(using = Base64Deserializer.class)
    private String exception;

    public StructuredFlowErrorEvent() {
    }

    public StructuredFlowErrorEvent(OperationDetails operation, FlowDetails flow, StackDetails stack, String exception) {
        this(operation, flow, stack, null, null, exception);
    }

    public StructuredFlowErrorEvent(OperationDetails operation, FlowDetails flow, StackDetails stack,
            ClusterDetails cluster, BlueprintDetails blueprint, String exception) {
        super(StructuredFlowErrorEvent.class.getSimpleName(), operation, flow, stack, cluster, blueprint);
        this.exception = exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    public String getException() {
        return exception;
    }
}
