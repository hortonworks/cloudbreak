package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.terminate;

import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseSelectableEvent;

public class ExternalDatabaseTerminationStartEvent extends ExternalDatabaseSelectableEvent {

    public ExternalDatabaseTerminationStartEvent(Long resourceId, String selector, String resourceName, String resourceCrn) {
        super(resourceId, selector, resourceName, resourceCrn);
    }
}
