package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.azure.AdlsCloudStorageParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.azure.AdlsGen2CloudStorageParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.azure.WasbCloudStorageParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.gcs.GcsCloudStorageParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.s3.S3CloudStorageParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.FileSystem;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class CloudStorageV4Base implements JsonEntity {

    @NotNull
    @ApiModelProperty(value = FileSystem.NAME, required = true)
    private String name;

    @NotNull
    @ApiModelProperty(value = FileSystem.TYPE, required = true)
    private String type;

    @ApiModelProperty(FileSystem.DEFAULT)
    private boolean defaultFs;

    @Valid
    @ApiModelProperty
    private AdlsCloudStorageParametersV4 adls;

    @Valid
    @ApiModelProperty
    private WasbCloudStorageParametersV4 wasb;

    @Valid
    @ApiModelProperty
    private GcsCloudStorageParametersV4 gcs;

    @Valid
    @ApiModelProperty
    private S3CloudStorageParametersV4 s3;

    @Valid
    @ApiModelProperty
    private AdlsGen2CloudStorageParametersV4 adlsGen2;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isDefaultFs() {
        return defaultFs;
    }

    public void setDefaultFs(boolean defaultFs) {
        this.defaultFs = defaultFs;
    }

    public AdlsCloudStorageParametersV4 getAdls() {
        return adls;
    }

    public void setAdls(AdlsCloudStorageParametersV4 adls) {
        this.adls = adls;
    }

    public WasbCloudStorageParametersV4 getWasb() {
        return wasb;
    }

    public void setWasb(WasbCloudStorageParametersV4 wasb) {
        this.wasb = wasb;
    }

    public GcsCloudStorageParametersV4 getGcs() {
        return gcs;
    }

    public void setGcs(GcsCloudStorageParametersV4 gcs) {
        this.gcs = gcs;
    }

    public S3CloudStorageParametersV4 getS3() {
        return s3;
    }

    public void setS3(S3CloudStorageParametersV4 s3) {
        this.s3 = s3;
    }

    public AdlsGen2CloudStorageParametersV4 getAdlsGen2() {
        return adlsGen2;
    }

    public void setAdlsGen2(AdlsGen2CloudStorageParametersV4 adlsGen2) {
        this.adlsGen2 = adlsGen2;
    }
}
