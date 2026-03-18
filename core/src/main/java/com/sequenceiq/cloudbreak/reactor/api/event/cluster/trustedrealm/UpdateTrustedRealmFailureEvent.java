package com.sequenceiq.cloudbreak.reactor.api.event.cluster.trustedrealm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public class UpdateTrustedRealmFailureEvent extends StackFailureEvent {

    @JsonCreator
    public UpdateTrustedRealmFailureEvent(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception) {
        super(EventSelectorUtil.failureSelector(UpdateTrustedRealmResult.class), stackId, exception);
    }
}

