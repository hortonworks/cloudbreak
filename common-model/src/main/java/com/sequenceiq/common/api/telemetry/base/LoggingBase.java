package com.sequenceiq.common.api.telemetry.base;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.WasbCloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.doc.TelemetryModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class LoggingBase implements Serializable {

    @NotNull
    @ApiModelProperty(TelemetryModelDescription.TELEMETRY_LOGGING_STORAGE_LOCATION)
    private String storageLocation;

    @ApiModelProperty(TelemetryModelDescription.TELEMETRY_LOGGING_S3_ATTRIBUTES)
    private S3CloudStorageV1Parameters s3;

    @ApiModelProperty(TelemetryModelDescription.TELEMETRY_LOGGING_WASB_ATTRIBUTES)
    private WasbCloudStorageV1Parameters wasb;

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

    public WasbCloudStorageV1Parameters getWasb() {
        return wasb;
    }

    public void setWasb(WasbCloudStorageV1Parameters wasb) {
        this.wasb = wasb;
    }
}
