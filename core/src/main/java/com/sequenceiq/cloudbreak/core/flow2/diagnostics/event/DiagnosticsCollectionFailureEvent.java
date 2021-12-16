package com.sequenceiq.cloudbreak.core.flow2.diagnostics.event;

import static com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionStateSelectors.FAILED_DIAGNOSTICS_COLLECTION_EVENT;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters;

public class DiagnosticsCollectionFailureEvent extends DiagnosticsCollectionEvent implements Selectable {

    private final Exception exception;

    private final String failureType;

    public DiagnosticsCollectionFailureEvent(Long resourceId, Exception exception, String resourceCrn, DiagnosticParameters parameters, String failureType) {
        super(FAILED_DIAGNOSTICS_COLLECTION_EVENT.name(), resourceId, resourceCrn, parameters, null, null, null);
        this.exception = exception;
        this.failureType = failureType;
    }

    @Override
    public String selector() {
        return FAILED_DIAGNOSTICS_COLLECTION_EVENT.name();
    }

    public Exception getException() {
        return exception;
    }

    public String getFailureType() {
        return failureType;
    }
}

