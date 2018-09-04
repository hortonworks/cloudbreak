package com.sequenceiq.cloudbreak.api.model;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.AbfsCloudStorageParameters;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.AdlsCloudStorageParameters;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.GcsCloudStorageParameters;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.S3CloudStorageParameters;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.WasbCloudStorageParameters;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.FileSystem;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class FileSystemBase implements JsonEntity {

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
    private AbfsCloudStorageParameters abfs;

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

    public AbfsCloudStorageParameters getAbfs() {
        return abfs;
    }

    public void setAbfs(AbfsCloudStorageParameters abfs) {
        this.abfs = abfs;
    }
}
