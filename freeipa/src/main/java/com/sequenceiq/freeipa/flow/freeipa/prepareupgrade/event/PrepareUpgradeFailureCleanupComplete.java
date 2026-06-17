package com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class PrepareUpgradeFailureCleanupComplete extends StackEvent {

    @JsonCreator
    public PrepareUpgradeFailureCleanupComplete(@JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
