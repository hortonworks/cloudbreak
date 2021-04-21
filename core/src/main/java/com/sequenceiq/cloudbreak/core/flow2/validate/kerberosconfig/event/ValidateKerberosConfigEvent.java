package com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.event;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ValidateKerberosConfigEvent extends StackEvent {

    private final boolean freeipaExistsForEnv;

    public ValidateKerberosConfigEvent(String selector, Long stackId, boolean freeipaExistsForEnv) {
        super(selector, stackId);
        this.freeipaExistsForEnv = freeipaExistsForEnv;
    }

    public boolean doesFreeipaExistsForEnv() {
        return freeipaExistsForEnv;
    }
}
