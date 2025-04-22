package com.sequenceiq.common.api.telemetry.base;

import java.io.Serializable;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.GcsCloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.doc.TelemetryModelDescription;
import com.sequenceiq.common.api.telemetry.model.CloudwatchParams;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class LoggingBase implements Serializable {

    @NotNull
    @Schema(description = TelemetryModelDescription.TELEMETRY_LOGGING_STORAGE_LOCATION)
    private String storageLocation;

    @Schema(description = TelemetryModelDescription.TELEMETRY_LOGGING_S3_ATTRIBUTES)
    private S3CloudStorageV1Parameters s3;

    @Schema(description = TelemetryModelDescription.TELEMETRY_LOGGING_ADLS_GEN_2_ATTRIBUTES)
    private AdlsGen2CloudStorageV1Parameters adlsGen2;

    @Schema(description = TelemetryModelDescription.TELEMETRY_LOGGING_GCS_ATTRIBUTES)
    private GcsCloudStorageV1Parameters gcs;

    @Deprecated
    @Schema(description = TelemetryModelDescription.TELEMETRY_LOGGING_CLOUDWATCH_ATTRIBUTES)
    private CloudwatchParams cloudwatch;

    public String getStorageLocation() {
        return storageLocation;
    }

    public void setStorageLocation(String storageLocation) {
        this.storageLocation = storageLocation;
    }

    public S3CloudStorageV1Parameters getS3() {
        return s3;
    }

    public void setS3(S3CloudStorageV1Parameters s3) {
        this.s3 = s3;
    }

    public AdlsGen2CloudStorageV1Parameters getAdlsGen2() {
        return adlsGen2;
    }

    public void setAdlsGen2(AdlsGen2CloudStorageV1Parameters adlsGen2) {
        this.adlsGen2 = adlsGen2;
    }

    public GcsCloudStorageV1Parameters getGcs() {
        return gcs;
    }

    public void setGcs(GcsCloudStorageV1Parameters gcs) {
        this.gcs = gcs;
    }

    @Deprecated
    public CloudwatchParams getCloudwatch() {
        return cloudwatch;
    }

    @Deprecated
    public void setCloudwatch(CloudwatchParams cloudwatch) {
        this.cloudwatch = cloudwatch;
    }

    @Override
    public String toString() {
        return "LoggingBase{" +
                "storageLocation='" + storageLocation + '\'' +
                ", s3=" + s3 +
                ", adlsGen2=" + adlsGen2 +
                ", gcs=" + gcs +
                '}';
    }
}
