package com.sequenceiq.freeipa.flow.stack.provision.event.userdata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class UpdateUserdataSecretsFailed extends StackFailureEvent {

    @JsonCreator
    public UpdateUserdataSecretsFailed(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception) {
        super(stackId, exception);
    }
}
