package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.cloud.model.storage.AdlsCloudStorageParameters;
import com.sequenceiq.cloudbreak.cloud.model.storage.AdlsGen2CloudStorageParameters;
import com.sequenceiq.cloudbreak.cloud.model.storage.GcsCloudStorageParameters;
import com.sequenceiq.cloudbreak.cloud.model.storage.S3CloudStorageParameters;
import com.sequenceiq.cloudbreak.cloud.model.storage.WasbCloudStorageParameters;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class CloudStorageV4Base implements JsonEntity {

    @Valid
    @ApiModelProperty
    private AdlsCloudStorageParameters adls;

    @Valid
    @ApiModelProperty
    private WasbCloudStorageParameters wasb;

    @Valid
    @ApiModelProperty
    private GcsCloudStorageParameters gcs;

    @Valid
    @ApiModelProperty
    private S3CloudStorageParameters s3;

    @Valid
    @ApiModelProperty
    private AdlsGen2CloudStorageParameters adlsGen2;

    public AdlsCloudStorageParameters getAdls() {
        return adls;
    }

    public void setAdls(AdlsCloudStorageParameters adls) {
        this.adls = adls;
    }

    public WasbCloudStorageParameters getWasb() {
        return wasb;
    }

    public void setWasb(WasbCloudStorageParameters wasb) {
        this.wasb = wasb;
    }

    public GcsCloudStorageParameters getGcs() {
        return gcs;
    }

    public void setGcs(GcsCloudStorageParameters gcs) {
        this.gcs = gcs;
    }

    public S3CloudStorageParameters getS3() {
        return s3;
    }

    public void setS3(S3CloudStorageParameters s3) {
        this.s3 = s3;
    }

    public AdlsGen2CloudStorageParameters getAdlsGen2() {
        return adlsGen2;
    }

    public void setAdlsGen2(AdlsGen2CloudStorageParameters adlsGen2) {
        this.adlsGen2 = adlsGen2;
    }
}
