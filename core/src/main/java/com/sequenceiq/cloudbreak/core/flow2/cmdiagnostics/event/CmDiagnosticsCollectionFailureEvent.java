package com.sequenceiq.cloudbreak.core.flow2.cmdiagnostics.event;

import static com.sequenceiq.cloudbreak.core.flow2.cmdiagnostics.event.CmDiagnosticsCollectionStateSelectors.FAILED_CM_DIAGNOSTICS_COLLECTION_EVENT;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.common.model.diagnostics.CmDiagnosticsParameters;

public class CmDiagnosticsCollectionFailureEvent extends CmDiagnosticsCollectionEvent implements Selectable {

    private final Exception exception;

    public CmDiagnosticsCollectionFailureEvent(Long resourceId, Exception exception,
            String resourceCrn, CmDiagnosticsParameters parameters) {
        super(FAILED_CM_DIAGNOSTICS_COLLECTION_EVENT.name(), resourceId, resourceCrn, parameters);
        this.exception = exception;
    }

    @Override
    public String selector() {
        return FAILED_CM_DIAGNOSTICS_COLLECTION_EVENT.name();
    }

    public Exception getException() {
        return exception;
    }
}
