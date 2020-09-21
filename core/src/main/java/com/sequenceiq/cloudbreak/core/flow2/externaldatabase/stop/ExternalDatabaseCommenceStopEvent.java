package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.stop;

import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseSelectableEvent;

public class ExternalDatabaseCommenceStopEvent extends ExternalDatabaseSelectableEvent {

    public ExternalDatabaseCommenceStopEvent(Long resourceId, String selector, String resourceName, String resourceCrn) {
        super(resourceId, selector, resourceName, resourceCrn);
    }
}
