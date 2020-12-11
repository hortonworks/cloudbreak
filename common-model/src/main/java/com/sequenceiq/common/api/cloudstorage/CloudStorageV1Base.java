package com.sequenceiq.common.api.cloudstorage;

import java.io.Serializable;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.api.cloudstorage.old.AdlsCloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.EfsCloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.GcsCloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.WasbCloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.validation.ValidCloudStorage;

import io.swagger.annotations.ApiModelProperty;

@ValidCloudStorage
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class CloudStorageV1Base implements Serializable {

    @Valid
    @ApiModelProperty
    private AdlsCloudStorageV1Parameters adls;

    @Valid
    @ApiModelProperty
    private WasbCloudStorageV1Parameters wasb;

    @Valid
    @ApiModelProperty
    private GcsCloudStorageV1Parameters gcs;

    @Valid
    @ApiModelProperty
    private S3CloudStorageV1Parameters s3;

    @Valid
    @ApiModelProperty
    private EfsCloudStorageV1Parameters efs;

    @Valid
    @ApiModelProperty
    private AdlsGen2CloudStorageV1Parameters adlsGen2;

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

    public GcsCloudStorageV1Parameters getGcs() {
        return gcs;
    }

    public void setGcs(GcsCloudStorageV1Parameters gcs) {
        this.gcs = gcs;
    }

    public S3CloudStorageV1Parameters getS3() {
        return s3;
    }

    public void setS3(S3CloudStorageV1Parameters s3) {
        this.s3 = s3;
    }

    public EfsCloudStorageV1Parameters getEfs() {
        return efs;
    }

    public void setEfs(EfsCloudStorageV1Parameters efs) {
        this.efs = efs;
    }

    public AdlsGen2CloudStorageV1Parameters getAdlsGen2() {
        return adlsGen2;
    }

    public void setAdlsGen2(AdlsGen2CloudStorageV1Parameters adlsGen2) {
        this.adlsGen2 = adlsGen2;
    }
}
