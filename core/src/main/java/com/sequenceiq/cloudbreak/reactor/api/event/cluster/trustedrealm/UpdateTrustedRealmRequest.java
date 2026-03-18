package com.sequenceiq.cloudbreak.reactor.api.event.cluster.trustedrealm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public class UpdateTrustedRealmRequest extends StackEvent {

    private final String environmentCrn;

    private final String realm;

    @JsonCreator
    public UpdateTrustedRealmRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("environmentCrn") String environmentCrn,
            @JsonProperty("realm") String realm) {
        super(EventSelectorUtil.selector(UpdateTrustedRealmRequest.class), stackId);
        this.environmentCrn = environmentCrn;
        this.realm = realm;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public String getRealm() {
        return realm;
    }
}

