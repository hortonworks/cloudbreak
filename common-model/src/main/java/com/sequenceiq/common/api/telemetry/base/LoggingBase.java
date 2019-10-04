package com.sequenceiq.common.api.telemetry.base;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
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

    @ApiModelProperty(TelemetryModelDescription.TELEMETRY_LOGGING_ADLS_GEN_2_ATTRIBUTES)
    private AdlsGen2CloudStorageV1Parameters adlsGen2;

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
}
