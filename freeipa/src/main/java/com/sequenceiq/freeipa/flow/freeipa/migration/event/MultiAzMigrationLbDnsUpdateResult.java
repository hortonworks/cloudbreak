package com.sequenceiq.freeipa.flow.freeipa.migration.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class MultiAzMigrationLbDnsUpdateResult extends StackEvent {

    private final String operationId;

    @JsonCreator
    public MultiAzMigrationLbDnsUpdateResult(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("operationId") String operationId) {
        super(resourceId);
        this.operationId = operationId;
    }

    public String getOperationId() {
        return operationId;
    }

    @Override
    public String toString() {
        return "MultiAzMigrationLbDnsUpdateResult{" +
                "operationId='" + operationId + '\'' +
                "} " + super.toString();
    }
}
