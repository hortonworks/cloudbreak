package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import java.util.HashSet;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.sequenceiq.common.model.JsonEntity;
import com.sequenceiq.common.api.cloudstorage.old.AdlsCloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.GcsCloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.WasbCloudStorageV1Parameters;
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
    private AdlsGen2CloudStorageV1Parameters adlsGen2;

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

    public AdlsGen2CloudStorageV1Parameters getAdlsGen2() {
        return adlsGen2;
    }

    public void setAdlsGen2(AdlsGen2CloudStorageV1Parameters adlsGen2) {
        this.adlsGen2 = adlsGen2;
    }
}
