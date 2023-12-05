package com.sequenceiq.freeipa.flow.freeipa.rebuild.event.backup;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class ValidateBackupFailed extends StackFailureEvent {
    @JsonCreator
    public ValidateBackupFailed(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception) {
        super(stackId, exception);
    }
}
