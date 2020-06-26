package com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.selection;

import java.util.List;

import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class ChangePrimaryGatewaySelectionRequest extends StackEvent {

    private final List<String> repairInstanceIds;

    public ChangePrimaryGatewaySelectionRequest(Long stackId, List<String> repairInstanceIds) {
        super(stackId);
        this.repairInstanceIds = repairInstanceIds;
    }

    public List<String> getRepairInstanceIds() {
        return repairInstanceIds;
    }
}
