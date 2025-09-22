package com.sequenceiq.common.api.telemetry.doc;

public class DiagnosticsModelDescription {
    public static final String ISSUE = "Issue number or JIRA ticket number related to this diagnostic collection request.";
    public static final String UUID = "Unique identifier for the diagnostics flow. If it's empty, flow ID will be used.";
    public static final String TICKET = "Optional ticket or case number for Cloudera Manager based diagnostic collection request.";
    public static final String LABELS = "With labels you can filter what kind of logs you'd like to collect.";
    public static final String START_TIME = "Start time for the time interval of the diagnostic collection request.";
    public static final String END_TIME = "END time for the time interval of the diagnostic collection request.";
    public static final String DESTINATION = "Destination for the diagnostic collection request.";
    public static final String DESCRIPTION = "Description of the diagnostics collection";
    public static final String HOSTS = "Host (fqdn) filter, use it to run diagnostics collection on only specific hosts";
    public static final String EXCLUDE_HOSTS = "Host (fqdn) filter, skip diagnostics on the specified hosts";
    public static final String HOST_GROUPS = "Host groups (instance groups), used it to run diagnostics collection only those " +
            "hosts that are included the specific host groups";
    public static final String ADDITIONAL_LOGS = "Additional log path and label pairs that will be sent in the diagnostics collection";
    public static final String INCLUDE_SALT_LOGS = "Include salt logs in the diagnostic collections";
    public static final String INCLUDE_SAR_OUTPUT = "Include sar outputs in the diagnostics collections";
    public static final String INCLUDE_NGINX_REPORT = "Include nginx html reports in the diagnostics collections";
    public static final String INCLUDE_SELINUX_REPORT = "Include SELinux report in the diagnostics collections";
    public static final String UPDATE_PACKAGE = "Upgrade or install required telemetry cli tool on the nodes (works only with network)";
    public static final String SKIP_VALIDATION = "Skip cloud storage write operation testing or databus connection " +
            "check (depends on the destination) during init stage.";
    public static final String SKIP_WORKSPACE_CLEANUP = "Skip workspace cleanup on the VM nodes at the start of the diagnostic ";
    public static final String SKIP_UNRESPONSIVE_HOSTS = "Skip unresponsive VM hosts from diagnostics";
    public static final String ROLES = "List of roles for which to get logs and metrics. If set, this restricts the roles for log and metrics collection " +
            "to the list specified. If empty, the default is to get logs for all roles.";
    public static final String BUNDLE_SIZE_BYTES = "The maximum approximate bundle size of the output file for CM based diagnostics collection.";
    public static final String COMMENTS = "Comments to include with this CM based data collection.";
    public static final String ENABLE_MONITOR_METRICS_COLLECTION = "Flag to enable collection of metrics for chart display in CM based diagnostics collection.";
    public static final String STACK_CRN = "Crn identifier of the stack for listing diagnostics collections.";

    private DiagnosticsModelDescription() {
    }
}
