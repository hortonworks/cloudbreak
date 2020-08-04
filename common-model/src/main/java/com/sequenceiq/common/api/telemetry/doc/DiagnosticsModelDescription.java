package com.sequenceiq.common.api.telemetry.doc;

public class DiagnosticsModelDescription {
    public static final String ISSUE = "Issue number or JIRA ticket number related to this diagnostic collection request.";
    public static final String LABELS = "With labels you can filter what kind of logs you'd like to collect.";
    public static final String START_TIME = "Start time for the time interval of the diagnostic collection request.";
    public static final String END_TIME = "END time for the time interval of the diagnostic collection request.";
    public static final String DESTINATION = "Destination for the diagnostic collection request.";
    public static final String DESCRIPTION = "description of the diagnostics collection";
    public static final String HOSTS = "Host (fqdn) filter, use it to run diagnostics collection on only specific hosts";
    public static final String HOST_GROUPS = "Host groups (instance groups), used it to run diagnostics collection only those " +
            "hosts that are included the specific host groups";
    public static final String ADDITIONAL_LOGS = "Additional log path and label pairs that will be sent in the diagnostics collection";
    public static final String INCLUDE_SALT_LOGS = "Include salt logs in the diagnostic collections";
    public static final String UPDATE_PACKAGE = "Upgrade or install required telemetry cli tool on the nodes (works only with network)";
    public static final String SKIP_VALIDATION = "Skip cloud storage write operation testing or databus connection " +
            "check (depends on the destination) during init stage.";

    private DiagnosticsModelDescription() {
    }
}
