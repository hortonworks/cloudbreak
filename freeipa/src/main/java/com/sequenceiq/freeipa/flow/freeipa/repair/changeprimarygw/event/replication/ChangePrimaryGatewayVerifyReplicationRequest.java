package com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.replication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class ChangePrimaryGatewayVerifyReplicationRequest extends StackEvent {

    @JsonCreator
    public ChangePrimaryGatewayVerifyReplicationRequest(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }

    @Override
    public String toString() {
        return "ChangePrimaryGatewayVerifyReplicationRequest{" +
                super.toString() +
                '}';
    }
}
