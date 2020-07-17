package com.sequenceiq.cloudbreak.core.flow2.diagnostics.event;

import static com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionStateSelectors.FAILED_DIAGNOSTICS_COLLECTION_EVENT;

import java.util.Map;

import com.sequenceiq.cloudbreak.common.event.Selectable;

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

