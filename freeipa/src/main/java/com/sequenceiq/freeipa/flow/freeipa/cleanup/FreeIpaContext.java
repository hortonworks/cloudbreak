package com.sequenceiq.freeipa.flow.freeipa.cleanup;

import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;

public class FreeIpaContext extends CommonContext {

    private final FreeIpa freeIpa;

    public FreeIpaContext(FlowParameters flowParameters, FreeIpa freeIpa) {
        super(flowParameters);
        this.freeIpa = freeIpa;
    }

    public Stack getStack() {
        return freeIpa.getStack();
    }

    public FreeIpa getFreeIpa() {
        return freeIpa;
    }
}
