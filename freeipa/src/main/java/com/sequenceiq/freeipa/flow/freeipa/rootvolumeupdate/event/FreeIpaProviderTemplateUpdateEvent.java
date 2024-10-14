package com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.event;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class FreeIpaProviderTemplateUpdateEvent extends StackEvent {

    private final String operationId;

    @JsonCreator
    public FreeIpaProviderTemplateUpdateEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("operationId") String operationId,
            @JsonProperty("resourceId") Long stackId) {
        super(selector, stackId);
        this.operationId = operationId;
    }

    public String getOperationId() {
        return operationId;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", FreeIpaProviderTemplateUpdateEvent.class.getSimpleName() + "[", "]")
                .add("selector=" + getSelector())
                .add("stackId=" + getResourceId())
                .add("operationId=" + operationId)
                .toString();
    }
}
