package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import java.util.HashSet;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.cloud.model.storage.AdlsCloudStorageParameters;
import com.sequenceiq.cloudbreak.cloud.model.storage.AdlsGen2CloudStorageParameters;
import com.sequenceiq.cloudbreak.cloud.model.storage.GcsCloudStorageParameters;
import com.sequenceiq.cloudbreak.cloud.model.storage.S3CloudStorageParameters;
import com.sequenceiq.cloudbreak.cloud.model.storage.WasbCloudStorageParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.location.StorageLocationV4Request;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.FileSystem;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class FileSystemValidationV4Request implements JsonEntity {

    @Valid
    @ApiModelProperty(FileSystem.LOCATIONS)
    private Set<StorageLocationV4Request> locations = new HashSet<>();

    @NotNull
    @ApiModelProperty(value = FileSystem.NAME, required = true)
    private String name;

    @NotNull
    @ApiModelProperty(value = FileSystem.TYPE, required = true)
    private String type;

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

    public Set<StorageLocationV4Request> getLocations() {
        return locations;
    }

    public void setLocations(Set<StorageLocationV4Request> locations) {
        this.locations = locations;
    }

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
