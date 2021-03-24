package com.sequenceiq.sdx.api.model.diagnostics.docs;

public final class DiagnosticsOperationDescriptions {

    public static final String COLLECT_DIAGNOSTICS = "Initiates the collection of diagnostical data on the hosts of the SDX";
    public static final String COLLECT_CM_DIAGNOSTICS = "Initiates the collection of diagnostical data through CM API. Requires a running CM server";
    public static final String GET_VM_LOG_PATHS = "Returns a list of log paths on the hosts of the SDX";
    public static final String GET_CM_ROLES = "Returns a list of CM Roles that can be used for filtering the diagnostics results. " +
            "Roles are immutable based on the deployment details";
    public static final String LIST_COLLECTIONS = "Returns a list of the recent diagnostics collections.";
    public static final String CANCEL_COLLECTIONS = "Cancel the not finished diagnostics collections.";

    private DiagnosticsOperationDescriptions() {
    }
}
