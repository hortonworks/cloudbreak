package com.sequenceiq.common.api.telemetry.doc;

public class TelemetryModelDescription {

    public static final String TELEMETRY_LOGGING = "Cloud Logging (telemetry) settings.";
    public static final String TELEMETRY_WORKLOAD_ANALYTICS = "Workload analytics (telemetry) settings.";
    public static final String TELEMETRY_WORKLOAD_ANALYTICS_ATTRIBUTES = "Workload analytics (telemetry) attributes.";
    public static final String TELEMETRY_FLUENT_ATTRIBUTES = "Telemetry fluent settings (overrides).";
    public static final String TELEMETRY_FEATURES = "Telemetry features settings";
    public static final String TELEMETRY_METERING = "Telemetry metering feature setting";
    public static final String TELEMETRY_LOGGING_S3_ATTRIBUTES = "telemetry - logging s3 attributes";
    public static final String TELEMETRY_LOGGING_ADLS_GEN_2_ATTRIBUTES = "telemetry - logging adls gen2 attributes";
    public static final String TELEMETRY_LOGGING_CLOUDWATCH_ATTRIBUTES = "telemetry - logging cloudwatch attributes";
    public static final String TELEMETRY_LOGGING_STORAGE_LOCATION = "telemetry - logging storage location / container";
    public static final String TELEMETRY_REPORT_DEPLOYMENT_LOGS_ENABLED = "enable cluster deployment log reporting.";
    public static final String TELEMETRY_CLOUDWATCH_PARAMS = "telemetry - cloudwatch releated parameters";
    public static final String TELEMETRY_CLOUDWATCH_PARAMS_REGION = "telemetry - cloudwatch related AWS region (should be used only outside of AWS platform)";

    private TelemetryModelDescription() {
    }

}
