package com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.selection;

import java.util.Optional;

import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class ChangePrimaryGatewaySelectionSuccess extends StackEvent {

    private final Optional<String> formerPrimaryGatewayInstanceId;

    private final String newPrimaryGatewayInstanceId;

    public ChangePrimaryGatewaySelectionSuccess(Long stackId, Optional<String> formerPrimaryGatewayInstanceId, String newPrimaryGatewayInstanceId) {
        super(stackId);
        this.formerPrimaryGatewayInstanceId = formerPrimaryGatewayInstanceId;
        this.newPrimaryGatewayInstanceId = newPrimaryGatewayInstanceId;
    }

    public Optional<String> getFormerPrimaryGatewayInstanceId() {
        return formerPrimaryGatewayInstanceId;
    }

    public String getNewPrimaryGatewayInstanceId() {
        return newPrimaryGatewayInstanceId;
    }
}
