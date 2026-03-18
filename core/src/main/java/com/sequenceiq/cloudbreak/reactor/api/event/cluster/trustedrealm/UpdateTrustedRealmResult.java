package com.sequenceiq.cloudbreak.reactor.api.event.cluster.trustedrealm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public class UpdateTrustedRealmResult extends StackEvent {

    @JsonCreator
    public UpdateTrustedRealmResult(@JsonProperty("resourceId") Long stackId) {
        super(EventSelectorUtil.selector(UpdateTrustedRealmResult.class), stackId);
    }
}

