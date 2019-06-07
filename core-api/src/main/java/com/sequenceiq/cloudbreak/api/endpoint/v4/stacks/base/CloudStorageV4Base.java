package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.AdlsCloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.AdlsGen2CloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.GcsCloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.S3CloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.WasbCloudStorageV4Parameters;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class CloudStorageV4Base implements JsonEntity {

    @Valid
    @ApiModelProperty
    private AdlsCloudStorageV4Parameters adls;

    @Valid
    @ApiModelProperty
    private WasbCloudStorageV4Parameters wasb;

    @Valid
    @ApiModelProperty
    private GcsCloudStorageV4Parameters gcs;

    @Valid
    @ApiModelProperty
    private S3CloudStorageV4Parameters s3;

    @Valid
    @ApiModelProperty
    private AdlsGen2CloudStorageV4Parameters adlsGen2;

    public AdlsCloudStorageV4Parameters getAdls() {
        return adls;
    }

    public void setAdls(AdlsCloudStorageV4Parameters adls) {
        this.adls = adls;
    }

    public WasbCloudStorageV4Parameters getWasb() {
        return wasb;
    }

    public void setWasb(WasbCloudStorageV4Parameters wasb) {
        this.wasb = wasb;
    }

    public GcsCloudStorageV4Parameters getGcs() {
        return gcs;
    }

    public void setGcs(GcsCloudStorageV4Parameters gcs) {
        this.gcs = gcs;
    }

    public S3CloudStorageV4Parameters getS3() {
        return s3;
    }

    public void setS3(S3CloudStorageV4Parameters s3) {
        this.s3 = s3;
    }

    public AdlsGen2CloudStorageV4Parameters getAdlsGen2() {
        return adlsGen2;
    }

    public void setAdlsGen2(AdlsGen2CloudStorageV4Parameters adlsGen2) {
        this.adlsGen2 = adlsGen2;
    }
}
