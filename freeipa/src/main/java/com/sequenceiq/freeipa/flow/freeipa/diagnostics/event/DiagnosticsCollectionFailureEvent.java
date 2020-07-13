package com.sequenceiq.freeipa.flow.freeipa.diagnostics.event;

import com.sequenceiq.cloudbreak.common.event.Selectable;

import java.util.Map;

import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionStateSelectors.FAILED_DIAGNOSTICS_COLLECTION_EVENT;

public class DiagnosticsCollectionFailureEvent extends DiagnosticsCollectionEvent implements Selectable {

    private final Exception exception;

    public DiagnosticsCollectionFailureEvent(Long resourceId, Exception exception, String resourceCrn, Map<String, Object> parameters) {
        super(FAILED_DIAGNOSTICS_COLLECTION_EVENT.name(), resourceId, resourceCrn, parameters);
        this.exception = exception;
    }

    @Override
    public String selector() {
        return FAILED_DIAGNOSTICS_COLLECTION_EVENT.name();
    }

    public Exception getException() {
        return exception;
    }
}

