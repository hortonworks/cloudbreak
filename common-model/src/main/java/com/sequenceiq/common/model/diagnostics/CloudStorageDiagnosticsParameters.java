package com.sequenceiq.common.model.diagnostics;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonSubTypes({ @JsonSubTypes.Type(value = AwsDiagnosticParameters.class, name = "awsDiagnosticParameters"),
        @JsonSubTypes.Type(value = AzureDiagnosticParameters.class, name = "azureDiagnosticParameters"),
        @JsonSubTypes.Type(value = GcsDiagnosticsParameters.class, name = "gcsDiagnosticsParameters") })
public interface CloudStorageDiagnosticsParameters {

    /**
     * Get cloud specific parameters and a key value map for diagnostics
     */
    Map<String, Object> toMap();
}
