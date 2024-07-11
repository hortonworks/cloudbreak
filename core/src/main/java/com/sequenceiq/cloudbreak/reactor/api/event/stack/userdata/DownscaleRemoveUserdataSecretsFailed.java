package com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class DownscaleRemoveUserdataSecretsFailed extends StackFailureEvent {

    @JsonCreator
    public DownscaleRemoveUserdataSecretsFailed(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception) {
        super(stackId, exception);
    }
}
