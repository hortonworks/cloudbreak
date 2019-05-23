package com.sequenceiq.distrox.api.v1.distrox.model.cluster.storage.parameter;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.FileSystem;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class CloudStorageV1Base implements JsonEntity {

    @ApiModelProperty(FileSystem.DEFAULT)
    private boolean defaultFs;

    @Valid
    @ApiModelProperty
    private AdlsCloudStorageV1Parameters adls;

    @Valid
    @ApiModelProperty
    private WasbCloudStorageV1Parameters wasb;

    @Valid
    @ApiModelProperty
    private S3CloudStorageV1Parameters s3;

    @Valid
    @ApiModelProperty
    private AdlsGen2CloudStorageV1Parameters adlsGen2;

    public boolean isDefaultFs() {
        return defaultFs;
    }

    public void setDefaultFs(boolean defaultFs) {
        this.defaultFs = defaultFs;
    }

    public AdlsCloudStorageV1Parameters getAdls() {
        return adls;
    }

    public void setAdls(AdlsCloudStorageV1Parameters adls) {
        this.adls = adls;
    }

    public WasbCloudStorageV1Parameters getWasb() {
        return wasb;
    }

    public void setWasb(WasbCloudStorageV1Parameters wasb) {
        this.wasb = wasb;
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
