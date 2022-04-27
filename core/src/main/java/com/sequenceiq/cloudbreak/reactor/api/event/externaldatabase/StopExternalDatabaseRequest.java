package com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase;

import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseSelectableEvent;

public class StopExternalDatabaseRequest extends ExternalDatabaseSelectableEvent {

    public StopExternalDatabaseRequest(Long resourceId, String selector, String resourceName, String resourceCrn) {
        super(resourceId, selector, resourceName, resourceCrn);
    }

}
