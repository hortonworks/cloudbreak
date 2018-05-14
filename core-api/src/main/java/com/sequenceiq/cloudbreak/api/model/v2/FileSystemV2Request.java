package com.sequenceiq.cloudbreak.api.model.v2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.api.model.filesystem.AdlsFileSystemParameters;
import com.sequenceiq.cloudbreak.api.model.filesystem.FileSystemParameters;
import com.sequenceiq.cloudbreak.api.model.filesystem.GcsFileSystemParameters;
import com.sequenceiq.cloudbreak.api.model.filesystem.S3FileSystemParameters;
import com.sequenceiq.cloudbreak.api.model.filesystem.WasbFileSystemParameters;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class FileSystemV2Request implements JsonEntity {

    @ApiModelProperty
    private String description;

    @ApiModelProperty
    private AdlsFileSystemParameters adls;

    @ApiModelProperty
    private WasbFileSystemParameters wasb;

    @ApiModelProperty
    private GcsFileSystemParameters gcs;

    @ApiModelProperty
    private S3FileSystemParameters s3;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AdlsFileSystemParameters getAdls() {
        return adls;
    }

    public void setAdls(AdlsFileSystemParameters adls) {
        this.adls = adls;
    }

    public WasbFileSystemParameters getWasb() {
        return wasb;
    }

    public void setWasb(WasbFileSystemParameters wasb) {
        this.wasb = wasb;
    }

    public GcsFileSystemParameters getGcs() {
        return gcs;
    }

    public void setGcs(GcsFileSystemParameters gcs) {
        this.gcs = gcs;
    }

    public S3FileSystemParameters getS3() {
        return s3;
    }

    public void setS3(S3FileSystemParameters s3) {
        this.s3 = s3;
    }

    public void fillFileSystemParametersRegardingToItsType(FileSystemParameters fileSystemParameters) {
        if (fileSystemParameters instanceof AdlsFileSystemParameters) {
            adls = (AdlsFileSystemParameters) fileSystemParameters;
        }
    }

}
