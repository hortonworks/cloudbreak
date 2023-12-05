package com.sequenceiq.freeipa.flow.freeipa.rebuild.event.backup;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class ValidateBackupSuccess extends StackEvent {
    @JsonCreator
    public ValidateBackupSuccess(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
