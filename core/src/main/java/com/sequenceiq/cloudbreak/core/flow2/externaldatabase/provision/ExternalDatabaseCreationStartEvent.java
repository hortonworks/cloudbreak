package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.provision;

import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseSelectableEvent;

public class ExternalDatabaseCreationStartEvent extends ExternalDatabaseSelectableEvent {

    public ExternalDatabaseCreationStartEvent(Long resourceId, String selector, String resourceName, String resourceCrn) {
        super(resourceId, selector, resourceName, resourceCrn);
    }
}
