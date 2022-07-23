package com.sequenceiq.cloudbreak.core.flow2.cmdiagnostics.event;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.CLASS;
import static com.sequenceiq.cloudbreak.core.flow2.cmdiagnostics.event.CmDiagnosticsCollectionStateSelectors.FAILED_CM_DIAGNOSTICS_COLLECTION_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.common.model.diagnostics.CmDiagnosticsParameters;

public class CmDiagnosticsCollectionFailureEvent extends CmDiagnosticsCollectionEvent implements Selectable {

    @JsonTypeInfo(use = CLASS, property = "@type")
    private final Exception exception;

    @JsonCreator
    public CmDiagnosticsCollectionFailureEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("parameters") CmDiagnosticsParameters parameters) {
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
