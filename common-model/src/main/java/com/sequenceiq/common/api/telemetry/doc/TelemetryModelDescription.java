package com.sequenceiq.common.api.telemetry.doc;

public class TelemetryModelDescription {

    public static final String TELEMETRY_LOGGING = "Cloud Logging (telemetry) settings.";
    public static final String TELEMETRY_WORKLOAD_ANALYTICS = "Workload analytics (telemetry) settings.";
    public static final String TELEMETRY_WORKLOAD_ANALYTICS_ATTRIBUTES = "Workload analytics (telemetry) attributes.";
    public static final String TELEMETRY_FLUENT_ATTRIBUTES = "Telemetry fluent settings (overrides).";
    public static final String TELEMETRY_FEATURES = "Telemetry features settings";
    public static final String TELEMETRY_RULES = "Telemetry anonymization rules (persistent on cluster level) that are applied on shipped logs.";
    public static final String TELEMETRY_METERING = "Telemetry metering feature setting";
    public static final String TELEMETRY_LOGGING_S3_ATTRIBUTES = "telemetry - logging s3 attributes";
    public static final String TELEMETRY_LOGGING_ADLS_GEN_2_ATTRIBUTES = "telemetry - logging adls gen2 attributes";
    public static final String TELEMETRY_LOGGING_GCS_ATTRIBUTES = "telemetry - logging gcs attributes";
    public static final String TELEMETRY_LOGGING_CLOUDWATCH_ATTRIBUTES = "telemetry - logging cloudwatch attributes";
    public static final String TELEMETRY_LOGGING_STORAGE_LOCATION = "telemetry - logging storage location / container";
    public static final String TELEMETRY_CLUSTER_LOGS_COLLECTION_ENABLED = "enable cluster logs collection";
    public static final String TELEMETRY_MONITORING = "Monitoring related (telemetry) settings.";
    public static final String TELEMETRY_MONITORING_REMOTE_WRITE_URL = "Monitoring prometheus endpoint for remote writes.";
    public static final String TELEMETRY_CLUSTER_MONITORING_ENABLED = "enable monitoring for cluster services";
    public static final String TELEMETRY_CLOUD_STORAGE_LOGGING_ENABLED = "enable uploading daemon service logs to cloud storage from the cluster nodes. " +
            "(default: enabled)";
    public static final String TELEMETRY_CLOUDWATCH_PARAMS = "telemetry - CloudWatch releated parameters";
    public static final String TELEMETRY_CLOUDWATCH_PARAMS_REGION = "telemetry - CloudWatch related AWS region (should be used only outside of AWS platform)";
    public static final String TELEMETRY_USE_SHARED_ALTUS_CREDENTIAL_ENABLED = "enable shared Altus credential usage";

    private TelemetryModelDescription() {
    }

}
