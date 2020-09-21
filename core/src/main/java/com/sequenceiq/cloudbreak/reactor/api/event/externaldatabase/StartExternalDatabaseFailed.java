package com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase;

import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseSelectableEvent;

public class StartExternalDatabaseFailed extends ExternalDatabaseSelectableEvent {

    private final Exception exception;

    public StartExternalDatabaseFailed(Long resourceId, String selector, String resourceName, String resourceCrn, Exception exception) {
        super(resourceId, selector, resourceName, resourceCrn);
        this.exception = exception;
    }

    public String selector() {
        return "StartExternalDatabaseFailed";
    }

    public Exception getException() {
        return exception;
    }

}
