package com.sequenceiq.freeipa.service.stack;

import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;

public class FreeIPADetails {

    private Stack stack;

    private FreeIpa freeIpa;

    public Stack getStack() {
        return stack;
    }

    public void setStack(Stack stack) {
        this.stack = stack;
    }

    public FreeIpa getFreeIpa() {
        return freeIpa;
    }

    public void setFreeIpa(FreeIpa freeIpa) {
        this.freeIpa = freeIpa;
    }
}
