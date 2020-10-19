package com.sequenceiq.common.model.diagnostics;

import java.util.Map;

public interface CloudStorageDiagnosticsParameters {

    /**
     * Get cloud specific parameters and a key value map for diagnostics
     */
    Map<String, Object> toMap();
}
