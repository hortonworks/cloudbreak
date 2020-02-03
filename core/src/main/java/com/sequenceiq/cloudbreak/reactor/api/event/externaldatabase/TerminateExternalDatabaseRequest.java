package com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase;

import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseSelectableEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

public class TerminateExternalDatabaseRequest extends ExternalDatabaseSelectableEvent {

    private final Stack stack;

    private final boolean forced;

    public TerminateExternalDatabaseRequest(Long resourceId, String selector, String resourceName, String resourceCrn, Stack stack, boolean forced) {
        super(resourceId, selector, resourceName, resourceCrn);
        this.stack = stack;
        this.forced = forced;
    }

    public Stack getStack() {
        return stack;
    }

    public boolean isForced() {
        return forced;
    }
}
