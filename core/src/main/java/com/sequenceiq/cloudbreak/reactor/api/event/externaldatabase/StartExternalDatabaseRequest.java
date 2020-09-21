package com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase;

import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseSelectableEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

public class StartExternalDatabaseRequest extends ExternalDatabaseSelectableEvent {

    private final Stack stack;

    public StartExternalDatabaseRequest(Long resourceId, String selector, String resourceName, String resourceCrn, Stack stack) {
        super(resourceId, selector, resourceName, resourceCrn);
        this.stack = stack;
    }

    public Stack getStack() {
        return stack;
    }

}
