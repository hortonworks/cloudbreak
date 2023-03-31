package com.sequenceiq.cloudbreak.reactor.api.event.kerberos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class KeytabConfigurationRequest extends StackEvent {

    private final boolean repair;

    @JsonCreator
    public KeytabConfigurationRequest(
            @JsonProperty("resourceId") Long stackId, @JsonProperty("repair") Boolean repair) {
        super(stackId);
        this.repair = repair != null && repair;
    }

    public boolean getRepair() {
        return repair;
    }
}
