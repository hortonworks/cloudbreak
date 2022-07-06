package com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ValidateKerberosConfigEvent extends StackEvent {

    private final boolean freeipaExistsForEnv;

    @JsonCreator
    public ValidateKerberosConfigEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("freeipaExistsForEnv") boolean freeipaExistsForEnv) {
        super(selector, stackId);
        this.freeipaExistsForEnv = freeipaExistsForEnv;
    }

    public boolean isFreeipaExistsForEnv() {
        return freeipaExistsForEnv;
    }
}
