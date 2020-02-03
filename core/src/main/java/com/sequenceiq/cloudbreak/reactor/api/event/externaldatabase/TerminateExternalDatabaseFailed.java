package com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase;

import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseSelectableEvent;

public class TerminateExternalDatabaseFailed extends ExternalDatabaseSelectableEvent {

    private final Exception exception;

    public TerminateExternalDatabaseFailed(Long resourceId, String selector, String resourceName, String resourceCrn, Exception exception) {
        super(resourceId, selector, resourceName, resourceCrn);
        this.exception = exception;
    }

    public String selector() {
        return "TerminateExternalDatabaseFailed";
    }

    public Exception getException() {
        return exception;
    }

}
