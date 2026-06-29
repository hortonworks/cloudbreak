package com.sequenceiq.freeipa.flow.freeipa.migration.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class MultiAzMigrationInitHandlerRequest extends StackEvent {

    private final String operationId;

    @JsonCreator
    public MultiAzMigrationInitHandlerRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("operationId") String operationId) {
        super(stackId);
        this.operationId = operationId;
    }

    public String getOperationId() {
        return operationId;
    }

    @Override
    public String toString() {
        return "MultiAzMigrationInitHandlerRequest{" +
                "operationId='" + operationId + '\'' +
                "} " + super.toString();
    }
}
