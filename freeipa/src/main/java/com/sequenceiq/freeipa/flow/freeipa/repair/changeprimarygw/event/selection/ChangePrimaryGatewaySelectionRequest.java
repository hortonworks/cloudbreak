package com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.selection;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class ChangePrimaryGatewaySelectionRequest extends StackEvent {

    private final List<String> repairInstanceIds;

    @JsonCreator
    public ChangePrimaryGatewaySelectionRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("repairInstanceIds") List<String> repairInstanceIds) {
        super(stackId);
        this.repairInstanceIds = repairInstanceIds;
    }

    public List<String> getRepairInstanceIds() {
        return repairInstanceIds;
    }
}
