package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.start;

import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseSelectableEvent;

public class ExternalDatabaseCommenceStartEvent extends ExternalDatabaseSelectableEvent {

    public ExternalDatabaseCommenceStartEvent(Long resourceId, String selector, String resourceName, String resourceCrn) {
        super(resourceId, selector, resourceName, resourceCrn);
    }
}
