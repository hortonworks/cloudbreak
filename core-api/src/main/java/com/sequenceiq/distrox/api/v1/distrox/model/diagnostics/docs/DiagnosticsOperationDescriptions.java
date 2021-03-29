package com.sequenceiq.distrox.api.v1.distrox.model.diagnostics.docs;

public final class DiagnosticsOperationDescriptions {

    public static final String COLLECT_DIAGNOSTICS = "Initiates the collection of diagnostical data on the hosts of Datahub";
    public static final String COLLECT_CM_DIAGNOSTICS = "Initiates the collection of diagnostical data on Datahub Cloudera Manager";
    public static final String GET_VM_LOG_PATHS = "Returns a list of log paths on the hosts of Datahub";
    public static final String GET_CM_ROLES = "Returns a list of Cloudera Manager service roles that can be used for Cloudera Manager based diagnostics";
    public static final String LIST_COLLECTIONS = "Returns a list of the recent diagnostics collections.";
    public static final String CANCEL_COLLECTIONS = "Cancel the not finished diagnostics collections.";

    private DiagnosticsOperationDescriptions() {
    }
}
