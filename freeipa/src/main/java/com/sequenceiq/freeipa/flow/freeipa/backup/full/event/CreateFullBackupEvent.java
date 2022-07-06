package com.sequenceiq.freeipa.flow.freeipa.backup.full.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class CreateFullBackupEvent extends StackEvent {
    @JsonCreator
    public CreateFullBackupEvent(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
