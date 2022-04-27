package com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase;

import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseSelectableEvent;

public class TerminateExternalDatabaseRequest extends ExternalDatabaseSelectableEvent {

    private final boolean forced;

    public TerminateExternalDatabaseRequest(Long resourceId, String selector, String resourceName, String resourceCrn, boolean forced) {
        super(resourceId, selector, resourceName, resourceCrn);
        this.forced = forced;
    }

    public boolean isForced() {
        return forced;
    }
}
